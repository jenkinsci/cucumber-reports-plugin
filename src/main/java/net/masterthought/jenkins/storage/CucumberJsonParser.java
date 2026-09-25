package net.masterthought.jenkins.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.masterthought.jenkins.storage.model.Attachment;
import net.masterthought.jenkins.storage.model.DataTableRow;
import net.masterthought.jenkins.storage.model.FeatureResult;
import net.masterthought.jenkins.storage.model.HookResult;
import net.masterthought.jenkins.storage.model.ScenarioResult;
import net.masterthought.jenkins.storage.model.StepResult;
import org.apache.commons.io.IOUtils;

/**
 * Parser for standard Cucumber JSON execution reports.
 * Compatible with Cucumber-JVM, Cucumber-JS, Behave, Playwright, and Cypress Cucumber formats.
 */
public class CucumberJsonParser {

    private static final Logger LOGGER = Logger.getLogger(CucumberJsonParser.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Parses a Cucumber JSON file into a list of FeatureResult objects.
     *
     * @param jsonFile The file to parse
     * @return List of parsed features
     * @throws IOException If parsing fails
     */
    public List<FeatureResult> parseFile(File jsonFile) throws IOException {
        try (InputStream is = new FileInputStream(jsonFile)) {
            return parseInputStream(is);
        }
    }

    /**
     * Parses Cucumber JSON content from an InputStream.
     */
    public List<FeatureResult> parseInputStream(InputStream inputStream) throws IOException {
        String jsonContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        return parseString(jsonContent);
    }

    /**
     * Parses a Cucumber JSON String.
     */
    public List<FeatureResult> parseString(String jsonContent) throws IOException {
        List<FeatureResult> features = new ArrayList<>();
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return features;
        }

        JsonNode rootNode = OBJECT_MAPPER.readTree(jsonContent);
        if (!rootNode.isArray()) {
            LOGGER.log(Level.WARNING, "Cucumber JSON root is not an array, skipping invalid payload.");
            return features;
        }

        AtomicInteger globalAttachmentCounter = new AtomicInteger(1);

        for (JsonNode featureNode : rootNode) {
            FeatureResult feature = parseFeature(featureNode, globalAttachmentCounter);
            features.add(feature);
        }

        return features;
    }

    private FeatureResult parseFeature(JsonNode featureNode, AtomicInteger attachmentCounter) {
        FeatureResult feature = new FeatureResult();
        feature.setId(getText(featureNode, "id"));
        feature.setName(getText(featureNode, "name", "Unnamed Feature"));
        feature.setUri(getText(featureNode, "uri"));
        feature.setDescription(getText(featureNode, "description"));
        feature.setKeyword(getText(featureNode, "keyword", "Feature"));
        feature.setLine(getInt(featureNode, "line", 0));

        // Parse Feature Tags
        List<String> tags = parseTags(featureNode.get("tags"));
        feature.setTags(tags);

        // Parse Elements (Scenarios & Backgrounds)
        JsonNode elementsNode = featureNode.get("elements");
        if (elementsNode != null && elementsNode.isArray()) {
            ScenarioResult currentBackground = null;

            for (JsonNode elementNode : elementsNode) {
                String type = getText(elementNode, "type", "scenario");
                if ("background".equalsIgnoreCase(type)) {
                    currentBackground = parseScenario(elementNode, feature.getName(), tags, attachmentCounter);
                } else {
                    ScenarioResult scenario = parseScenario(elementNode, feature.getName(), tags, attachmentCounter);
                    // If there is a preceding background, prepend its steps to the scenario for complete context
                    if (currentBackground != null && currentBackground.getSteps() != null) {
                        List<StepResult> combinedSteps = new ArrayList<>(currentBackground.getSteps());
                        combinedSteps.addAll(scenario.getSteps());
                        scenario.setSteps(combinedSteps);
                    }
                    feature.getScenarios().add(scenario);
                }
            }
        }

        feature.setStatus(feature.computeStatus());
        feature.setDuration(feature.computeDuration());
        return feature;
    }

    private ScenarioResult parseScenario(JsonNode elementNode, String featureName, List<String> inheritedTags, AtomicInteger attachmentCounter) {
        ScenarioResult scenario = new ScenarioResult();
        scenario.setId(getText(elementNode, "id"));
        scenario.setName(getText(elementNode, "name", "Unnamed Scenario"));
        scenario.setDescription(getText(elementNode, "description"));
        scenario.setKeyword(getText(elementNode, "keyword", "Scenario"));
        scenario.setType(getText(elementNode, "type", "scenario"));
        scenario.setLine(getInt(elementNode, "line", 0));
        scenario.setFeatureName(featureName);

        // Combine Feature-level tags and Scenario-level tags
        List<String> scenarioTags = new ArrayList<>(inheritedTags);
        List<String> explicitTags = parseTags(elementNode.get("tags"));
        for (String tag : explicitTags) {
            if (!scenarioTags.contains(tag)) {
                scenarioTags.add(tag);
            }
        }
        scenario.setTags(scenarioTags);

        // Parse Before Hooks
        scenario.setBeforeHooks(parseHooks(elementNode.get("before"), "before", attachmentCounter));

        // Parse Steps
        JsonNode stepsNode = elementNode.get("steps");
        if (stepsNode != null && stepsNode.isArray()) {
            for (JsonNode stepNode : stepsNode) {
                StepResult step = parseStep(stepNode, attachmentCounter);
                scenario.getSteps().add(step);
            }
        }

        // Parse After Hooks
        scenario.setAfterHooks(parseHooks(elementNode.get("after"), "after", attachmentCounter));

        // Compute Status & Duration
        scenario.setStatus(scenario.computeStatus());
        scenario.setDuration(scenario.computeDuration());

        // Extract error message & stack trace from failing steps or hooks
        for (HookResult hook : scenario.getBeforeHooks()) {
            if ("failed".equalsIgnoreCase(hook.getStatus())) {
                scenario.setErrorMessage(hook.getErrorMessage());
                scenario.setErrorStackTrace(hook.getErrorStackTrace());
                break;
            }
        }
        if (scenario.getErrorMessage() == null) {
            for (StepResult step : scenario.getSteps()) {
                if ("failed".equalsIgnoreCase(step.getStatus())) {
                    scenario.setErrorMessage(step.getErrorMessage());
                    scenario.setErrorStackTrace(step.getErrorStackTrace());
                    break;
                }
            }
        }
        if (scenario.getErrorMessage() == null) {
            for (HookResult hook : scenario.getAfterHooks()) {
                if ("failed".equalsIgnoreCase(hook.getStatus())) {
                    scenario.setErrorMessage(hook.getErrorMessage());
                    scenario.setErrorStackTrace(hook.getErrorStackTrace());
                    break;
                }
            }
        }

        return scenario;
    }

    private StepResult parseStep(JsonNode stepNode, AtomicInteger attachmentCounter) {
        StepResult step = new StepResult();
        step.setKeyword(getText(stepNode, "keyword", "").trim());
        step.setName(getText(stepNode, "name", ""));
        step.setLine(getInt(stepNode, "line", 0));

        // Result object
        JsonNode resultNode = stepNode.get("result");
        if (resultNode != null) {
            step.setStatus(getText(resultNode, "status", "passed"));
            step.setDuration(getLong(resultNode, "duration", 0L));
            step.setErrorMessage(getText(resultNode, "error_message"));
            step.setErrorStackTrace(getText(resultNode, "error_message"));
        } else {
            step.setStatus("skipped");
        }

        // DocString
        JsonNode docStringNode = stepNode.get("doc_string");
        if (docStringNode != null) {
            step.setDocString(getText(docStringNode, "value"));
        }

        // Data Table rows
        JsonNode rowsNode = stepNode.get("rows");
        if (rowsNode != null && rowsNode.isArray()) {
            List<DataTableRow> rows = new ArrayList<>();
            for (JsonNode rowNode : rowsNode) {
                JsonNode cellsNode = rowNode.get("cells");
                if (cellsNode != null && cellsNode.isArray()) {
                    List<String> cells = new ArrayList<>();
                    for (JsonNode cellNode : cellsNode) {
                        cells.add(cellNode.asText(""));
                    }
                    rows.add(new DataTableRow(cells));
                }
            }
            step.setRows(rows);
        }

        // Embeddings / Attachments
        step.setAttachments(parseAttachments(stepNode.get("embeddings"), attachmentCounter));
        if (step.getAttachments().isEmpty()) {
            step.setAttachments(parseAttachments(stepNode.get("attachments"), attachmentCounter));
        }

        return step;
    }

    private List<HookResult> parseHooks(JsonNode hooksNode, String hookType, AtomicInteger attachmentCounter) {
        List<HookResult> hooks = new ArrayList<>();
        if (hooksNode != null && hooksNode.isArray()) {
            for (JsonNode hookNode : hooksNode) {
                HookResult hook = new HookResult();
                hook.setType(hookType);
                JsonNode resultNode = hookNode.get("result");
                if (resultNode != null) {
                    hook.setStatus(getText(resultNode, "status", "passed"));
                    hook.setDuration(getLong(resultNode, "duration", 0L));
                    hook.setErrorMessage(getText(resultNode, "error_message"));
                    hook.setErrorStackTrace(getText(resultNode, "error_message"));
                } else {
                    hook.setStatus("passed");
                }
                hook.setAttachments(parseAttachments(hookNode.get("embeddings"), attachmentCounter));
                hooks.add(hook);
            }
        }
        return hooks;
    }

    private List<Attachment> parseAttachments(JsonNode embeddingsNode, AtomicInteger attachmentCounter) {
        List<Attachment> attachments = new ArrayList<>();
        if (embeddingsNode != null && embeddingsNode.isArray()) {
            for (JsonNode embNode : embeddingsNode) {
                String rawName = getText(embNode, "name");
                String name = (rawName != null && !rawName.trim().isEmpty())
                        ? rawName.trim()
                        : "Attachment - " + attachmentCounter.getAndIncrement();

                String mimeType = getText(embNode, "mime_type");
                if (mimeType == null) {
                    mimeType = getText(embNode, "media.type", "text/plain");
                }
                String data = getText(embNode, "data");

                Attachment attachment = new Attachment(name, mimeType, data);
                attachments.add(attachment);
            }
        }
        return attachments;
    }

    private List<String> parseTags(JsonNode tagsNode) {
        List<String> tags = new ArrayList<>();
        if (tagsNode != null && tagsNode.isArray()) {
            for (JsonNode tagNode : tagsNode) {
                String name = getText(tagNode, "name");
                if (name != null && !name.trim().isEmpty()) {
                    String clean = name.trim();
                    if (!tags.contains(clean)) {
                        tags.add(clean);
                    }
                }
            }
        }
        return tags;
    }

    private String getText(JsonNode node, String fieldName) {
        return getText(node, fieldName, null);
    }

    private String getText(JsonNode node, String fieldName, String defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : defaultValue;
    }

    private int getInt(JsonNode node, String fieldName, int defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asInt(defaultValue) : defaultValue;
    }

    private long getLong(JsonNode node, String fieldName, long defaultValue) {
        if (node == null) {
            return defaultValue;
        }
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asLong(defaultValue) : defaultValue;
    }
}
