package net.masterthought.jenkins.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Prunes large attachments (such as large screenshots and voluminous console logs)
 * directly from cached JSON files before ReportBuilder generates static HTML reports.
 * This prevents static HTML files from duplicating multi-megabyte Base64 embeddings
 * across feature, tag, and failure overview pages.
 */
public class AttachmentPruner {

    private static final Logger LOGGER = Logger.getLogger(AttachmentPruner.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 1x1 transparent dummy PNG so ReportBuilder's Base64 image decoding succeeds
     * and writes a minimal placeholder to disk without crashing.
     */
    public static final String DUMMY_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

    public static void pruneJsonFiles(List<String> jsonFilePaths, int attachmentThresholdKB) {
        if (jsonFilePaths == null || jsonFilePaths.isEmpty()) {
            return;
        }

        long thresholdBytes = (long) (attachmentThresholdKB > 0 ? attachmentThresholdKB : 500) * 1024L;

        for (String filePath : jsonFilePaths) {
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                continue;
            }

            try {
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
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to prune attachments from JSON: " + file.getName(), e);
            }
        }
    }

    private static boolean pruneHookEmbeddings(JsonNode hooksNode, long thresholdBytes) {
        boolean modified = false;
        if (hooksNode != null && hooksNode.isArray()) {
            for (JsonNode hook : hooksNode) {
                modified |= pruneEmbeddingsNode(hook.get("embeddings"), thresholdBytes);
            }
        }
        return modified;
    }

    private static boolean pruneEmbeddingsNode(JsonNode embeddingsNode, long thresholdBytes) {
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
                            String mimeType = obj.has("mime_type") ? obj.get("mime_type").asText() : "";
                            String existingName = obj.has("name") ? obj.get("name").asText() : "Attachment";
                            long sizeKB = data.length() / 1024;
                            obj.put("name", existingName + " [Pruned: " + sizeKB + " KB]");

                            if (mimeType.startsWith("image/")) {
                                obj.put("data", DUMMY_PNG_BASE64);
                            } else {
                                String truncatedNote = "[Attachment (" + sizeKB + " KB) pruned to reduce report size]";
                                obj.put("data", Base64.getEncoder().encodeToString(truncatedNote.getBytes(StandardCharsets.UTF_8)));
                            }
                            obj.put("pruned", true);
                            modified = true;
                        }
                    }
                }
            }
        }
        return modified;
    }
}
