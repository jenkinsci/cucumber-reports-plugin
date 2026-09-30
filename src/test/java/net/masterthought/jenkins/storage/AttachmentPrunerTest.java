package net.masterthought.jenkins.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AttachmentPrunerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testPruningLargeAttachments(@TempDir File tempDir) throws IOException {
        File jsonFile = new File(tempDir, "cucumber.json");

        String largeData = "A".repeat(1500); // 1.5 KB
        String smallData = "small text attachment";

        String jsonContent = "[\n" +
                "  {\n" +
                "    \"id\": \"feat1\",\n" +
                "    \"name\": \"Feature 1\",\n" +
                "    \"elements\": [\n" +
                "      {\n" +
                "        \"id\": \"scen1\",\n" +
                "        \"name\": \"Scenario 1\",\n" +
                "        \"before\": [\n" +
                "          {\n" +
                "            \"embeddings\": [\n" +
                "              {\n" +
                "                \"mime_type\": \"image/png\",\n" +
                "                \"data\": \"" + largeData + "\",\n" +
                "                \"name\": \"Large Screenshot\"\n" +
                "              }\n" +
                "            ]\n" +
                "          }\n" +
                "        ],\n" +
                "        \"steps\": [\n" +
                "          {\n" +
                "            \"name\": \"step 1\",\n" +
                "            \"embeddings\": [\n" +
                "              {\n" +
                "                \"mime_type\": \"text/plain\",\n" +
                "                \"data\": \"" + Base64.getEncoder().encodeToString(largeData.getBytes(StandardCharsets.UTF_8)) + "\",\n" +
                "                \"name\": \"Large Log\"\n" +
                "              },\n" +
                "              {\n" +
                "                \"mime_type\": \"text/plain\",\n" +
                "                \"data\": \"" + smallData + "\",\n" +
                "                \"name\": \"Small Log\"\n" +
                "              }\n" +
                "            ]\n" +
                "          }\n" +
                "        ],\n" +
                "        \"after\": [\n" +
                "          {\n" +
                "            \"embeddings\": [\n" +
                "              {\n" +
                "                \"mime_type\": \"image/png\",\n" +
                "                \"data\": \"" + smallData + "\",\n" +
                "                \"name\": \"Small Screenshot\"\n" +
                "              }\n" +
                "            ]\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]";

        FileUtils.writeStringToFile(jsonFile, jsonContent, StandardCharsets.UTF_8);

        // Prune with 1 KB threshold (1024 bytes)
        AttachmentPruner.pruneJsonFiles(Collections.singletonList(jsonFile.getAbsolutePath()), 1);

        JsonNode root = mapper.readTree(jsonFile);
        JsonNode beforeEmb = root.get(0).get("elements").get(0).get("before").get(0).get("embeddings").get(0);
        assertTrue(beforeEmb.get("pruned").asBoolean());
        assertEquals(AttachmentPruner.DUMMY_PNG_BASE64, beforeEmb.get("data").asText());
        assertTrue(beforeEmb.get("name").asText().contains("[Pruned:"));

        JsonNode stepEmbs = root.get(0).get("elements").get(0).get("steps").get(0).get("embeddings");
        JsonNode largeLogEmb = stepEmbs.get(0);
        assertTrue(largeLogEmb.get("pruned").asBoolean());
        String decodedText = new String(Base64.getDecoder().decode(largeLogEmb.get("data").asText()), StandardCharsets.UTF_8);
        assertTrue(decodedText.contains("[Attachment"));

        JsonNode smallLogEmb = stepEmbs.get(1);
        assertFalse(smallLogEmb.has("pruned"));
        assertEquals(smallData, smallLogEmb.get("data").asText());

        JsonNode afterEmb = root.get(0).get("elements").get(0).get("after").get(0).get("embeddings").get(0);
        assertFalse(afterEmb.has("pruned"));
        assertEquals(smallData, afterEmb.get("data").asText());
    }

    @Test
    public void testPruningGracefulEdgeCases(@TempDir File tempDir) {
        // Null and empty list
        AttachmentPruner.pruneJsonFiles(null, 500);
        AttachmentPruner.pruneJsonFiles(Collections.emptyList(), 500);

        // Non-existent file
        AttachmentPruner.pruneJsonFiles(Collections.singletonList(new File(tempDir, "missing.json").getAbsolutePath()), 500);

        // Non-JSON / invalid format
        File invalidFile = new File(tempDir, "invalid.json");
        try {
            FileUtils.writeStringToFile(invalidFile, "not json", StandardCharsets.UTF_8);
            AttachmentPruner.pruneJsonFiles(Collections.singletonList(invalidFile.getAbsolutePath()), 500);
        } catch (IOException ignored) {
        }
    }
}
