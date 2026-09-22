package net.masterthought.jenkins.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    public void testNonExistentGzipPayloadHandling() throws Exception {
        File emptyDir = tempDir.resolve("empty-build").toFile();
        emptyDir.mkdirs();

        assertNull(StorageOptimizer.loadPayloadFromGzip(emptyDir));
        assertEquals("{}", StorageOptimizer.readRawJsonFromGzip(emptyDir));

        StorageOptimizer optimizer = new StorageOptimizer(emptyDir, true, -10);
        assertNotNull(optimizer.getExternalizer());
    }

    @Test
    public void testReadRawJsonFromExistingGzip() throws Exception {
        File buildDir = tempDir.resolve("json-build").toFile();
        buildDir.mkdirs();

        StorageOptimizer optimizer = new StorageOptimizer(buildDir, false, 500);
        CucumberReportPayload payload = new CucumberReportPayload();
        ReportSummary summary = new ReportSummary();
        summary.setTotalFeatures(1);
        payload.setSummary(summary);
        optimizer.savePayloadToGzip(payload);

        String rawJson = StorageOptimizer.readRawJsonFromGzip(buildDir);
        assertNotNull(rawJson);
        assertTrue(rawJson.contains("\"totalFeatures\":1"));
    }

    @Test
    public void testOptimizeNullFeaturesAndSkippedStatuses() throws Exception {
        File buildDir = tempDir.resolve("branch-build").toFile();
        buildDir.mkdirs();

        StorageOptimizer optimizer = new StorageOptimizer(buildDir, false, 500);

        // 1. Test null features
        CucumberReportPayload p1 = optimizer.optimizeAndSave(null, "NullBuild", "/job/null/1");
        assertEquals(0, p1.getSummary().getTotalFeatures());

        // 2. Test features with skipped, pending, undefined and null attachment data
        FeatureResult skippedFeat = new FeatureResult();
        skippedFeat.setStatus("skipped");
        skippedFeat.setDuration(100L);
        skippedFeat.setTags(Collections.singletonList("skipped-tag"));

        net.masterthought.jenkins.storage.model.ScenarioResult sc = new net.masterthought.jenkins.storage.model.ScenarioResult();
        sc.setStatus("skipped");

        net.masterthought.jenkins.storage.model.StepResult pendingStep = new net.masterthought.jenkins.storage.model.StepResult();
        pendingStep.setStatus("pending");
        // Attachment with null data
        pendingStep.setAttachments(Collections.singletonList(new net.masterthought.jenkins.storage.model.Attachment("nullAtt", "text/plain", null)));

        net.masterthought.jenkins.storage.model.StepResult undefStep = new net.masterthought.jenkins.storage.model.StepResult();
        undefStep.setStatus("undefined");

        net.masterthought.jenkins.storage.model.StepResult skipStep = new net.masterthought.jenkins.storage.model.StepResult();
        skipStep.setStatus("skipped");

        sc.setSteps(java.util.Arrays.asList(pendingStep, undefStep, skipStep));
        skippedFeat.setScenarios(Collections.singletonList(sc));

        CucumberReportPayload p2 = optimizer.optimizeAndSave(Collections.singletonList(skippedFeat), "SkipBuild", "/job/skip/1");
        assertEquals(1, p2.getSummary().getSkippedFeatures());
        assertEquals(1, p2.getSummary().getSkippedScenarios());
        assertEquals(3, p2.getSummary().getSkippedSteps());
    }

    @Test
    public void testPruneCachedJsonFilesEdgeCases() throws Exception {
        File buildDir = tempDir.resolve("prune-edge-build").toFile();
        buildDir.mkdirs();

        StorageOptimizer optimizer = new StorageOptimizer(buildDir, false, 1);

        // 1. null list and empty list
        optimizer.pruneCachedJsonFiles(null);
        optimizer.pruneCachedJsonFiles(Collections.emptyList());

        // 2. Non-existent file and directory
        File missingFile = tempDir.resolve("missing.json").toFile();
        File aDir = tempDir.resolve("just-a-dir").toFile();
        aDir.mkdirs();
        optimizer.pruneCachedJsonFiles(java.util.Arrays.asList(missingFile.getAbsolutePath(), aDir.getAbsolutePath()));

        // 3. Non-array JSON file
        File objFile = tempDir.resolve("object.json").toFile();
        Files.write(objFile.toPath(), "{\"key\": \"value\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        optimizer.pruneCachedJsonFiles(Collections.singletonList(objFile.getAbsolutePath()));

        // 4. Non-image embedding (text/plain) exceeding threshold
        File textEmbFile = tempDir.resolve("text-emb.json").toFile();
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            largeText.append("Log line number ").append(i).append(" with verbose stack trace details\n");
        }
        String jsonWithTextEmb = "[\n" +
                "  {\n" +
                "    \"elements\": [\n" +
                "      {\n" +
                "        \"before\": [\n" +
                "          {\n" +
                "            \"embeddings\": [\n" +
                "              {\"mime_type\": \"text/plain\", \"data\": \"" + largeText.toString().replace("\n", "\\n") + "\"}\n" +
                "            ]\n" +
                "          }\n" +
                "        ],\n" +
                "        \"steps\": [\n" +
                "          {\n" +
                "            \"attachments\": [\n" +
                "              {\"name\": \"shortAtt\", \"mime_type\": \"text/plain\", \"data\": \"" + largeText.toString().replace("\n", "\\n") + "\"}\n" +
                "            ]\n" +
                "          }\n" +
                "        ],\n" +
                "        \"after\": [\n" +
                "          {\n" +
                "            \"embeddings\": [\n" +
                "              {\"mime_type\": \"text/plain\", \"data\": \"" + largeText.toString().replace("\n", "\\n") + "\"}\n" +
                "            ]\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]";

        Files.write(textEmbFile.toPath(), jsonWithTextEmb.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        optimizer.pruneCachedJsonFiles(Collections.singletonList(textEmbFile.getAbsolutePath()));

        // Verify that the text embedding was pruned and replaced with external reference
        String prunedContent = new String(Files.readAllBytes(textEmbFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(prunedContent.contains("externalized"));
        assertTrue(prunedContent.contains("att-"));
    }
}


