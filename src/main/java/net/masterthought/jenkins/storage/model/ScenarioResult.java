package net.masterthought.jenkins.storage.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a Cucumber Scenario execution result.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScenarioResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;
    private String keyword;
    private String type;
    private int line;
    private List<String> tags = new ArrayList<>();
    private List<StepResult> steps = new ArrayList<>();
    private List<HookResult> beforeHooks = new ArrayList<>();
    private List<HookResult> afterHooks = new ArrayList<>();
    private String status; // passed, failed, skipped
    private long duration;
    private String errorMessage;
    private String errorStackTrace;
    private String featureName;

    public ScenarioResult() {
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public List<StepResult> getSteps() {
        return steps;
    }

    public void setSteps(List<StepResult> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
    }

    public List<HookResult> getBeforeHooks() {
        return beforeHooks;
    }

    public void setBeforeHooks(List<HookResult> beforeHooks) {
        this.beforeHooks = beforeHooks != null ? beforeHooks : new ArrayList<>();
    }

    public List<HookResult> getAfterHooks() {
        return afterHooks;
    }

    public void setAfterHooks(List<HookResult> afterHooks) {
        this.afterHooks = afterHooks != null ? afterHooks : new ArrayList<>();
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorStackTrace() {
        return errorStackTrace;
    }

    public void setErrorStackTrace(String errorStackTrace) {
        this.errorStackTrace = errorStackTrace;
    }

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    public String computeStatus() {
        for (HookResult hook : beforeHooks) {
            if ("failed".equalsIgnoreCase(hook.getStatus())) {
                return "failed";
            }
        }
        for (StepResult step : steps) {
            if ("failed".equalsIgnoreCase(step.getStatus())) {
                return "failed";
            }
        }
        for (HookResult hook : afterHooks) {
            if ("failed".equalsIgnoreCase(hook.getStatus())) {
                return "failed";
            }
        }
        for (StepResult step : steps) {
            String st = step.getStatus().toLowerCase(java.util.Locale.ROOT);
            if ("skipped".equals(st) || "pending".equals(st) || "undefined".equals(st)) {
                return "skipped";
            }
        }
        return "passed";
    }

    public long computeDuration() {
        long total = 0;
        for (HookResult hook : beforeHooks) {
            total += hook.getDuration();
        }
        for (StepResult step : steps) {
            total += step.getDuration();
        }
        for (HookResult hook : afterHooks) {
            total += hook.getDuration();
        }
        return total;
    }
}
