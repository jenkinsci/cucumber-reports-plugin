package net.masterthought.jenkins.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.masterthought.jenkins.storage.model.Attachment;
import net.masterthought.jenkins.storage.model.CucumberReportPayload;
import net.masterthought.jenkins.storage.model.FeatureResult;
import net.masterthought.jenkins.storage.model.HookResult;
import net.masterthought.jenkins.storage.model.ReportSummary;
import net.masterthought.jenkins.storage.model.ScenarioResult;
import net.masterthought.jenkins.storage.model.StepResult;
import org.apache.commons.io.IOUtils;

/**
 * Core engine responsible for Jenkins controller storage optimization:
 * 1. GZIP compression on the fly into build.getRootDir()/cucumber-report.json.gz
 * 2. Pruning and externalizing large attachments from cached JSON files to fragment files
 * 3. Calculating summary metrics and structuring the lightweight report payload.
 */
public class StorageOptimizer {

    private static final Logger LOGGER = Logger.getLogger(StorageOptimizer.class.getName());
    public static final String REPORT_GZ_FILENAME = "cucumber-report.json.gz";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final File buildRootDir;
    private final boolean embedFullAttachments;
    private final int attachmentThresholdKB;
    private final AttachmentExternalizer externalizer;

    public StorageOptimizer(File buildRootDir, boolean embedFullAttachments, int attachmentThresholdKB) {
        this.buildRootDir = buildRootDir;
        this.embedFullAttachments = embedFullAttachments;
        this.attachmentThresholdKB = attachmentThresholdKB > 0 ? attachmentThresholdKB : 500;
        this.externalizer = new AttachmentExternalizer(buildRootDir);
    }

    /**
     * Optimizes, summarizes, and compresses parsed features into cucumber-report.json.gz.
     *
     * @param features List of parsed feature results
     * @param buildDisplayName Name of the current Jenkins build
     * @param buildUrl Relative URL of the build
     * @return Processed report payload
     * @throws IOException If disk write or compression fails
     */
    public CucumberReportPayload optimizeAndSave(List<FeatureResult> features, String buildDisplayName, String buildUrl) throws IOException {
        long thresholdBytes = (long) attachmentThresholdKB * 1024L;

        ReportSummary summary = new ReportSummary();
        int totalScenarios = 0;
        int passedScenarios = 0;
        int failedScenarios = 0;
        int skippedScenarios = 0;

        int totalSteps = 0;
        int passedSteps = 0;
        int failedSteps = 0;
        int skippedSteps = 0;

        int totalPassedFeatures = 0;
        int totalFailedFeatures = 0;
        int totalSkippedFeatures = 0;

        long totalDurationNanos = 0;
        int totalAttachmentsCount = 0;
        Set<String> uniqueTags = new HashSet<>();

        if (features == null) {
            features = Collections.emptyList();
        }

        // Process attachments and compute aggregates
        for (FeatureResult feature : features) {
            uniqueTags.addAll(feature.getTags());
            totalDurationNanos += feature.getDuration();

            String fStatus = feature.getStatus();
            if ("failed".equalsIgnoreCase(fStatus)) {
                totalFailedFeatures++;
            } else if ("skipped".equalsIgnoreCase(fStatus)) {
                totalSkippedFeatures++;
            } else {
                totalPassedFeatures++;
            }

            for (ScenarioResult scenario : feature.getScenarios()) {
                uniqueTags.addAll(scenario.getTags());
                totalScenarios++;

                String sStatus = scenario.getStatus();
                if ("failed".equalsIgnoreCase(sStatus)) {
                    failedScenarios++;
                } else if ("skipped".equalsIgnoreCase(sStatus)) {
                    skippedScenarios++;
                } else {
                    passedScenarios++;
                }

                // Process Before Hook Attachments
                for (HookResult hook : scenario.getBeforeHooks()) {
                    totalAttachmentsCount += processAttachments(hook.getAttachments(), thresholdBytes);
                }

                // Process Steps & Attachments
                for (StepResult step : scenario.getSteps()) {
                    totalSteps++;
                    String stepStatus = step.getStatus();
                    if ("failed".equalsIgnoreCase(stepStatus)) {
                        failedSteps++;
                    } else if ("skipped".equalsIgnoreCase(stepStatus) || "pending".equalsIgnoreCase(stepStatus) || "undefined".equalsIgnoreCase(stepStatus)) {
                        skippedSteps++;
                    } else {
                        passedSteps++;
                    }

                    totalAttachmentsCount += processAttachments(step.getAttachments(), thresholdBytes);
                }

                // Process After Hook Attachments
                for (HookResult hook : scenario.getAfterHooks()) {
                    totalAttachmentsCount += processAttachments(hook.getAttachments(), thresholdBytes);
                }
            }
        }

        // Finalize Summary
        summary.setTotalFeatures(features.size());
        summary.setPassedFeatures(totalPassedFeatures);
        summary.setFailedFeatures(totalFailedFeatures);
        summary.setSkippedFeatures(totalSkippedFeatures);

        summary.setTotalScenarios(totalScenarios);
        summary.setPassedScenarios(passedScenarios);
        summary.setFailedScenarios(failedScenarios);
        summary.setSkippedScenarios(skippedScenarios);

        summary.setTotalSteps(totalSteps);
        summary.setPassedSteps(passedSteps);
        summary.setFailedSteps(failedSteps);
        summary.setSkippedSteps(skippedSteps);

        summary.setTotalDuration(totalDurationNanos);
        summary.setTotalAttachments(totalAttachmentsCount);

        List<String> sortedTags = new ArrayList<>(uniqueTags);
        Collections.sort(sortedTags);
        summary.setAllTags(sortedTags);

        // Build Payload
        CucumberReportPayload payload = new CucumberReportPayload();
        payload.setSummary(summary);
        payload.setFeatures(features);
        payload.setEmbedFullAttachments(embedFullAttachments);
        payload.setAttachmentThresholdKB(attachmentThresholdKB);
        payload.setBuildDisplayName(buildDisplayName);
        payload.setBuildUrl(buildUrl);

        // Serialize and compress directly to disk
        savePayloadToGzip(payload);

        return payload;
    }

    private int processAttachments(List<Attachment> attachments, long thresholdBytes) {
        if (attachments == null || attachments.isEmpty()) {
            return 0;
        }

        for (Attachment att : attachments) {
            String data = att.getData();
            if (data == null) {
                continue;
            }

            long sizeBytes = data.length();
            att.setSizeBytes(sizeBytes);

            if (!embedFullAttachments) {
                // If attachment exceeds threshold
                if (sizeBytes > thresholdBytes) {
                    try {
                        String fileId = externalizer.saveAttachment(data);
                        att.setExternal(true);
                        att.setExternalFileId(fileId);

                        // Truncate preview to 200 chars if not fully embedded
                        if (sizeBytes > 200) {
                            String preview = data.substring(0, 200) + "...";
                            att.setData(preview);
                            att.setTruncated(true);
                        }
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Failed to externalize attachment fragment: " + att.getName(), e);
                    }
                }
            }
        }
        return attachments.size();
    }

    /**
     * Inspects cached JSON files and prunes large embeddings directly on disk before
     * ReportBuilder generates HTML files. This prevents massive HTML reports.
     *
     * @param jsonFilePaths List of file paths inside the cache directory
     * @throws IOException If file reading or writing fails
     */
    public void pruneCachedJsonFiles(List<String> jsonFilePaths) throws IOException {
        if (jsonFilePaths == null || embedFullAttachments) {
            return;
        }

        long thresholdBytes = (long) attachmentThresholdKB * 1024L;

        for (String filePath : jsonFilePaths) {
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                continue;
            }

            JsonNode rootNode = OBJECT_MAPPER.readTree(file);
            if (!rootNode.isArray()) {
                continue;
            }

            boolean modified = false;
            for (JsonNode featureNode : rootNode) {
                JsonNode elements = featureNode.get("elements");
                if (elements != null && elements.isArray()) {
                    for (JsonNode elemNode : elements) {
                        modified |= pruneHookEmbeddings(elemNode.get("before"), thresholdBytes);
                        JsonNode steps = elemNode.get("steps");
                        if (steps != null && steps.isArray()) {
                            for (JsonNode stepNode : steps) {
                                modified |= pruneEmbeddingsNode(stepNode.get("embeddings"), thresholdBytes);
                                modified |= pruneEmbeddingsNode(stepNode.get("attachments"), thresholdBytes);
                            }
                        }
                        modified |= pruneHookEmbeddings(elemNode.get("after"), thresholdBytes);
                    }
                }
            }

            if (modified) {
                OBJECT_MAPPER.writeValue(file, rootNode);
                LOGGER.log(Level.FINE, "Pruned large attachments from cached JSON: {0}", file.getName());
            }
        }
    }

    private boolean pruneHookEmbeddings(JsonNode hooksNode, long thresholdBytes) {
        boolean modified = false;
        if (hooksNode != null && hooksNode.isArray()) {
            for (JsonNode hook : hooksNode) {
                modified |= pruneEmbeddingsNode(hook.get("embeddings"), thresholdBytes);
            }
        }
        return modified;
    }

    private boolean pruneEmbeddingsNode(JsonNode embeddingsNode, long thresholdBytes) {
        boolean modified = false;
        if (embeddingsNode != null && embeddingsNode.isArray()) {
            ArrayNode arr = (ArrayNode) embeddingsNode;
            for (int i = 0; i < arr.size(); i++) {
                JsonNode emb = arr.get(i);
                if (emb instanceof ObjectNode) {
                    ObjectNode obj = (ObjectNode) emb;
                    JsonNode dataNode = obj.get("data");
                    if (dataNode != null && dataNode.isTextual()) {
                        String data = dataNode.asText();
                        if (data.length() > thresholdBytes) {
                            try {
                                String fileId = externalizer.saveAttachment(data);
                                String mimeType = obj.has("mime_type") ? obj.get("mime_type").asText() : "";
                                String note = "[Attachment (" + (data.length() / 1024) + " KB) externalized to " + fileId + "]";
                                String existingName = obj.has("name") ? obj.get("name").asText() : "Attachment";
                                obj.put("name", existingName + " (" + fileId + ")");

                                if (mimeType.startsWith("image/")) {
                                    // 1x1 transparent dummy PNG so ReportBuilder's Base64 decoder succeeds and writes tiny file
                                    obj.put("data", "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
                                } else {
                                    String preview = (data.length() > 200 ? data.substring(0, 200) : data) + "\n" + note;
                                    obj.put("data", java.util.Base64.getEncoder().encodeToString(preview.getBytes(StandardCharsets.UTF_8)));
                                }
                                obj.put("externalized", true);
                                obj.put("externalFileId", fileId);
                                modified = true;
                            } catch (IOException e) {
                                LOGGER.log(Level.WARNING, "Failed to externalize cached embedding", e);
                            }
                        }
                    }
                }
            }
        }
        return modified;
    }

    /**
     * Serializes payload to cucumber-report.json.gz on disk.
     */
    public void savePayloadToGzip(CucumberReportPayload payload) throws IOException {
        File targetGzFile = new File(buildRootDir, REPORT_GZ_FILENAME);
        try (OutputStream fos = new FileOutputStream(targetGzFile);
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
            OBJECT_MAPPER.writeValue(gzos, payload);
        }
        LOGGER.log(Level.FINE, "Saved compressed Cucumber report to: {0} ({1} bytes)",
                new Object[]{targetGzFile.getAbsolutePath(), targetGzFile.length()});
    }

    /**
     * Reads and decompresses the report payload from cucumber-report.json.gz on disk.
     */
    public static CucumberReportPayload loadPayloadFromGzip(File buildRootDir) throws IOException {
        File gzFile = new File(buildRootDir, REPORT_GZ_FILENAME);
        if (!gzFile.exists()) {
            return null;
        }
        try (InputStream fis = new FileInputStream(gzFile);
             GZIPInputStream gzis = new GZIPInputStream(fis)) {
            return OBJECT_MAPPER.readValue(gzis, CucumberReportPayload.class);
        }
    }

    /**
     * Reads decompressed JSON string directly from cucumber-report.json.gz.
     */
    public static String readRawJsonFromGzip(File buildRootDir) throws IOException {
        File gzFile = new File(buildRootDir, REPORT_GZ_FILENAME);
        if (!gzFile.exists()) {
            return "{}";
        }
        try (InputStream fis = new FileInputStream(gzFile);
             GZIPInputStream gzis = new GZIPInputStream(fis)) {
            return IOUtils.toString(gzis, StandardCharsets.UTF_8);
        }
    }

    public AttachmentExternalizer getExternalizer() {
        return externalizer;
    }
}
