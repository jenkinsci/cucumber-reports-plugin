package net.masterthought.jenkins.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import net.masterthought.cucumber.Reportable;
import net.masterthought.jenkins.storage.model.CucumberReportPayload;
import net.masterthought.jenkins.storage.model.FeatureResult;
import net.masterthought.jenkins.storage.model.ReportSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class StorageOptimizerTest {

    @TempDir
    Path tempDir;

    @Test
    public void testOptimizeAndSaveGzip() throws Exception {
        File buildRootDir = tempDir.resolve("build-1").toFile();
        buildRootDir.mkdirs();
        CucumberJsonParser parser = new CucumberJsonParser();
        List<FeatureResult> features;

        try (InputStream is = getClass().getResourceAsStream("/sample-cucumber.json")) {
            assertNotNull(is, "sample-cucumber.json resource must be available");
            features = parser.parseInputStream(is);
        }

        // Run StorageOptimizer with threshold 1KB to test pruning & externalization
        StorageOptimizer optimizer = new StorageOptimizer(buildRootDir, false, 1);
        CucumberReportPayload payload = optimizer.optimizeAndSave(features, "Build #1", "/job/test/1");

        // Verify summary
        ReportSummary summary = payload.getSummary();
        assertEquals(2, summary.getTotalFeatures());
        assertEquals(4, summary.getTotalScenarios());
        assertEquals(2, summary.getPassedScenarios());
        assertEquals(1, summary.getFailedScenarios());
        assertEquals(1, summary.getSkippedScenarios());
        assertEquals(50.0, summary.getPassPercentage(), 0.01);

        // Verify GZIP file was written on disk
        File gzFile = new File(buildRootDir, StorageOptimizer.REPORT_GZ_FILENAME);
        assertTrue(gzFile.exists(), "cucumber-report.json.gz must exist");
        assertTrue(gzFile.length() > 0, "cucumber-report.json.gz size must be > 0");

        // Decompress and verify integrity
        CucumberReportPayload loaded = StorageOptimizer.loadPayloadFromGzip(buildRootDir);
        assertNotNull(loaded);
        assertEquals(summary.getTotalScenarios(), loaded.getSummary().getTotalScenarios());

        // Verify Attachment Externalization
        File attDir = new File(buildRootDir, AttachmentExternalizer.ATTACHMENTS_DIR_NAME);
        assertTrue(attDir.exists(), "Attachment directory must exist");
        File[] fragments = attDir.listFiles();
        assertNotNull(fragments);
        assertTrue(fragments.length > 0, "At least one fragment must have been written");
        AttachmentExternalizer externalizer = new AttachmentExternalizer(buildRootDir);
        String content = externalizer.readAttachment(fragments[0].getName());
        assertNotNull(content);
        assertFalse(content.isEmpty());
    }

    @Test
    public void testPruneCachedJsonFiles() throws Exception {
        File buildRootDir = tempDir.resolve("build-2").toFile();
        buildRootDir.mkdirs();

        Path cacheFile = tempDir.resolve("cached-cucumber.json");
        try (InputStream is = getClass().getResourceAsStream("/sample-cucumber.json")) {
            assertNotNull(is);
            Files.copy(is, cacheFile, StandardCopyOption.REPLACE_EXISTING);
        }

        StorageOptimizer optimizer = new StorageOptimizer(buildRootDir, false, 1);
        optimizer.pruneCachedJsonFiles(Collections.singletonList(cacheFile.toFile().getAbsolutePath()));

        // Inspect the pruned cached file
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(cacheFile.toFile());
        assertTrue(rootNode.isArray());

        // Check if attachments have been externalized and replaced with preview
        JsonNode steps = rootNode.get(0).get("elements").get(1).get("steps");
        JsonNode failingStep = steps.get(1);
        JsonNode embeddings = failingStep.get("embeddings");
        assertNotNull(embeddings);
        assertTrue(embeddings.isArray());
        assertTrue(embeddings.size() > 0);

        JsonNode firstEmbedding = embeddings.get(0);
        assertTrue(firstEmbedding.has("externalized") && firstEmbedding.get("externalized").asBoolean(), "Must be marked externalized");
        assertTrue(firstEmbedding.has("externalFileId"), "Must have externalFileId");
        assertTrue(firstEmbedding.get("name").asText().contains("att-"), "Name must contain external file reference");

        // Verify that the external attachment fragment file exists
        File attDir = new File(buildRootDir, AttachmentExternalizer.ATTACHMENTS_DIR_NAME);
        assertTrue(attDir.exists(), "Attachment directory must be created");
        File[] fragments = attDir.listFiles();
        assertNotNull(fragments);
        assertTrue(fragments.length > 0, "At least one fragment must have been written");
    }

    @Test
    public void testEndToEndReportBuilderWithPrunedJson() throws Exception {
        File buildRootDir = tempDir.resolve("build-e2e").toFile();
        buildRootDir.mkdirs();

        Path cacheFile = tempDir.resolve("e2e-cucumber.json");
        try (InputStream is = getClass().getResourceAsStream("/sample-cucumber.json")) {
            assertNotNull(is);
            Files.copy(is, cacheFile, StandardCopyOption.REPLACE_EXISTING);
        }

        // 1. Optimize and prune
        StorageOptimizer optimizer = new StorageOptimizer(buildRootDir, false, 1);
        optimizer.pruneCachedJsonFiles(Collections.singletonList(cacheFile.toFile().getAbsolutePath()));

        // 2. Run ReportBuilder (the real Velocity HTML generator)
        Configuration configuration = new Configuration(buildRootDir, "TestProject");
        configuration.setBuildNumber("1");
        ReportBuilder reportBuilder = new ReportBuilder(Collections.singletonList(cacheFile.toFile().getAbsolutePath()), configuration);
        Reportable result = reportBuilder.generateReports();

        assertNotNull(result, "Report generation must complete successfully");
        assertEquals(2, result.getFeatures(), "Features count must match");
        assertEquals(4, result.getScenarios(), "Scenarios count must match");

        // 3. Save compressed GZIP payload as well
        optimizer.optimizeAndSave(new CucumberJsonParser().parseFile(cacheFile.toFile()), "Build #1", "/job/test/1");

        // 4. Copy to target/manual-test-report for easy manual inspection by user
        File manualReportDir = new File("target/manual-test-report");
        org.apache.commons.io.FileUtils.deleteQuietly(manualReportDir);
        org.apache.commons.io.FileUtils.copyDirectory(buildRootDir, manualReportDir);

        // 5. Verify HTML output directory structure
        File reportDir = new File(buildRootDir, "cucumber-html-reports");
        assertTrue(reportDir.exists(), "cucumber-html-reports directory must exist");

        // Overviews
        File overviewFeatures = new File(reportDir, "overview-features.html");
        assertTrue(overviewFeatures.exists(), "overview-features.html must be generated");
        File overviewTags = new File(reportDir, "overview-tags.html");
        assertTrue(overviewTags.exists(), "overview-tags.html must be generated");
        File overviewFailures = new File(reportDir, "overview-failures.html");
        assertTrue(overviewFailures.exists(), "overview-failures.html must be generated");
        File overviewSteps = new File(reportDir, "overview-steps.html");
        assertTrue(overviewSteps.exists(), "overview-steps.html must be generated");

        // Tag reports (report-tag_*.html)
        File[] tagFiles = reportDir.listFiles((dir, name) -> name.startsWith("report-tag_") && name.endsWith(".html"));
        assertNotNull(tagFiles, "Tag report files array must not be null");
        assertTrue(tagFiles.length > 0, "Separate report-tag_*.html files must be generated");

        // Feature reports (report-feature_*.html)
        File[] featureFiles = reportDir.listFiles((dir, name) -> name.startsWith("report-feature_") && name.endsWith(".html"));
        assertNotNull(featureFiles, "Feature report files array must not be null");
        assertTrue(featureFiles.length > 0, "Separate report-feature_*.html files must be generated");
    }
}
