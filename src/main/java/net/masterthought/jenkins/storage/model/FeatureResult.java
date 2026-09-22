package net.masterthought.jenkins.storage.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a BDD Feature and its scenarios.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeatureResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String uri;
    private String description;
    private String keyword;
    private int line;
    private List<String> tags = new ArrayList<>();
    private List<ScenarioResult> scenarios = new ArrayList<>();
    private String status; // passed, failed, skipped
    private long duration;

    public FeatureResult() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public List<ScenarioResult> getScenarios() {
        return scenarios;
    }

    public void setScenarios(List<ScenarioResult> scenarios) {
        this.scenarios = scenarios != null ? scenarios : new ArrayList<>();
    }

    public String getStatus() {
        if (status != null) {
            return status.toLowerCase(java.util.Locale.ROOT);
        }
        return computeStatus();
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getDuration() {
        if (duration > 0) {
            return duration;
        }
        return computeDuration();
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String computeStatus() {
        boolean hasSkipped = false;
        for (ScenarioResult sc : scenarios) {
            String scStatus = sc.getStatus();
            if ("failed".equalsIgnoreCase(scStatus)) {
                return "failed";
            }
            if ("skipped".equalsIgnoreCase(scStatus)) {
                hasSkipped = true;
            }
        }
        return hasSkipped && scenarios.size() > 0 ? "skipped" : "passed";
    }

    public long computeDuration() {
        long total = 0;
        for (ScenarioResult sc : scenarios) {
            total += sc.getDuration();
        }
        return total;
    }

    public int getPassedScenariosCount() {
        int count = 0;
        for (ScenarioResult sc : scenarios) {
            if ("passed".equalsIgnoreCase(sc.getStatus())) {
                count++;
            }
        }
        return count;
    }

    public int getFailedScenariosCount() {
        int count = 0;
        for (ScenarioResult sc : scenarios) {
            if ("failed".equalsIgnoreCase(sc.getStatus())) {
                count++;
            }
        }
        return count;
    }

    public int getSkippedScenariosCount() {
        int count = 0;
        for (ScenarioResult sc : scenarios) {
            String st = sc.getStatus();
            if ("skipped".equalsIgnoreCase(st) || "pending".equalsIgnoreCase(st) || "undefined".equalsIgnoreCase(st)) {
                count++;
            }
        }
        return count;
    }
}
