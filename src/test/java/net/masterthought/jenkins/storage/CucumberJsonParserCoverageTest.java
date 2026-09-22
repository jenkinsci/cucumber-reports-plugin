package net.masterthought.jenkins.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.masterthought.jenkins.storage.model.FeatureResult;
import net.masterthought.jenkins.storage.model.ScenarioResult;
import net.masterthought.jenkins.storage.model.StepResult;
import org.junit.jupiter.api.Test;

public class CucumberJsonParserCoverageTest {

    private final CucumberJsonParser parser = new CucumberJsonParser();

    @Test
    public void testEmptyAndInvalidJsonString() throws Exception {
        assertTrue(parser.parseString(null).isEmpty());
        assertTrue(parser.parseString("").isEmpty());
        assertTrue(parser.parseString("   ").isEmpty());

        // Non-array JSON
        assertTrue(parser.parseString("{\"key\": \"value\"}").isEmpty());
    }

    @Test
    public void testBackgroundElementAndDataTableAndDocString() throws Exception {
        String json = "[\n" +
                "  {\n" +
                "    \"id\": \"feat-bg\",\n" +
                "    \"name\": \"Feature with Background\",\n" +
                "    \"uri\": \"features/bg.feature\",\n" +
                "    \"elements\": [\n" +
                "      {\n" +
                "        \"type\": \"background\",\n" +
                "        \"name\": \"Common Setup\",\n" +
                "        \"steps\": [\n" +
                "          {\n" +
                "            \"keyword\": \"Given \",\n" +
                "            \"name\": \"background step\",\n" +
                "            \"result\": {\"status\": \"passed\", \"duration\": 100}\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"type\": \"scenario\",\n" +
                "        \"name\": \"Actual Scenario\",\n" +
                "        \"steps\": [\n" +
                "          {\n" +
                "            \"keyword\": \"When \",\n" +
                "            \"name\": \"action with docstring and data table\",\n" +
                "            \"result\": {\"status\": \"passed\", \"duration\": 200},\n" +
                "            \"doc_string\": {\"value\": \"sample docstring text\"},\n" +
                "            \"rows\": [\n" +
                "              {\"cells\": [\"user\", \"role\"]},\n" +
                "              {\"cells\": [\"alice\", \"admin\"]}\n" +
                "            ],\n" +
                "            \"attachments\": [\n" +
                "              {\"name\": \"log\", \"mime_type\": \"text/plain\", \"data\": \"SGVsbG8=\"}\n" +
                "            ]\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]";

        List<FeatureResult> features = parser.parseString(json);
        assertEquals(1, features.size());

        FeatureResult feature = features.get(0);
        assertEquals(1, feature.getScenarios().size());

        ScenarioResult scenario = feature.getScenarios().get(0);
        // Should have 2 steps: 1 background step + 1 scenario step
        assertEquals(2, scenario.getSteps().size());

        StepResult secondStep = scenario.getSteps().get(1);
        assertEquals("sample docstring text", secondStep.getDocString());
        assertEquals(2, secondStep.getRows().size());
        assertEquals("user", secondStep.getRows().get(0).getCells().get(0));
        assertEquals(1, secondStep.getAttachments().size());
    }

    @Test
    public void testHookFailuresAndMissingStepResult() throws Exception {
        String json = "[\n" +
                "  {\n" +
                "    \"id\": \"feat-hooks\",\n" +
                "    \"name\": \"Feature with Failing Hooks\",\n" +
                "    \"elements\": [\n" +
                "      {\n" +
                "        \"type\": \"scenario\",\n" +
                "        \"name\": \"Scenario failing in before hook\",\n" +
                "        \"before\": [\n" +
                "          {\n" +
                "            \"result\": {\"status\": \"failed\", \"error_message\": \"Before hook failed\"}\n" +
                "          }\n" +
                "        ],\n" +
                "        \"steps\": [\n" +
                "          {\n" +
                "            \"name\": \"step with missing result object\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"after\": [\n" +
                "          {\n" +
                "            \"result\": {\"status\": \"failed\", \"error_message\": \"After hook failed\"}\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]";

        List<FeatureResult> features = parser.parseString(json);
        assertEquals(1, features.size());

        ScenarioResult sc = features.get(0).getScenarios().get(0);
        assertEquals("failed", sc.getStatus());
        assertEquals("Before hook failed", sc.getErrorMessage());

        // Step without result should default to skipped
        StepResult step = sc.getSteps().get(0);
        assertEquals("skipped", step.getStatus());
    }
}
