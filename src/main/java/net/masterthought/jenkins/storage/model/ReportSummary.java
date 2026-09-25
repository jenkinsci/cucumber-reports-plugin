package net.masterthought.jenkins.storage.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Summary metrics of the Cucumber test execution run.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    private int totalFeatures;
    private int passedFeatures;
    private int failedFeatures;
    private int skippedFeatures;

    private int totalScenarios;
    private int passedScenarios;
    private int failedScenarios;
    private int skippedScenarios;

    private int totalSteps;
    private int passedSteps;
    private int failedSteps;
    private int skippedSteps;

    private long totalDuration; // in nanoseconds
    private String formattedDuration;
    private double passPercentage;
    private List<String> allTags = new ArrayList<>();
    private int totalAttachments;
    private long timestamp = System.currentTimeMillis();

    public ReportSummary() {
    }

    public int getTotalFeatures() {
        return totalFeatures;
    }

    public void setTotalFeatures(int totalFeatures) {
        this.totalFeatures = totalFeatures;
    }

    public int getPassedFeatures() {
        return passedFeatures;
    }

    public void setPassedFeatures(int passedFeatures) {
        this.passedFeatures = passedFeatures;
    }

    public int getFailedFeatures() {
        return failedFeatures;
    }

    public void setFailedFeatures(int failedFeatures) {
        this.failedFeatures = failedFeatures;
    }

    public int getSkippedFeatures() {
        return skippedFeatures;
    }

    public void setSkippedFeatures(int skippedFeatures) {
        this.skippedFeatures = skippedFeatures;
    }

    public int getTotalScenarios() {
        return totalScenarios;
    }

    public void setTotalScenarios(int totalScenarios) {
        this.totalScenarios = totalScenarios;
    }

    public int getPassedScenarios() {
        return passedScenarios;
    }

    public void setPassedScenarios(int passedScenarios) {
        this.passedScenarios = passedScenarios;
    }

    public int getFailedScenarios() {
        return failedScenarios;
    }

    public void setFailedScenarios(int failedScenarios) {
        this.failedScenarios = failedScenarios;
    }

    public int getSkippedScenarios() {
        return skippedScenarios;
    }

    public void setSkippedScenarios(int skippedScenarios) {
        this.skippedScenarios = skippedScenarios;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public int getPassedSteps() {
        return passedSteps;
    }

    public void setPassedSteps(int passedSteps) {
        this.passedSteps = passedSteps;
    }

    public int getFailedSteps() {
        return failedSteps;
    }

    public void setFailedSteps(int failedSteps) {
        this.failedSteps = failedSteps;
    }

    public int getSkippedSteps() {
        return skippedSteps;
    }

    public void setSkippedSteps(int skippedSteps) {
        this.skippedSteps = skippedSteps;
    }

    public long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(long totalDuration) {
        this.totalDuration = totalDuration;
        this.formattedDuration = formatDuration(totalDuration);
    }

    public String getFormattedDuration() {
        if (formattedDuration == null && totalDuration > 0) {
            formattedDuration = formatDuration(totalDuration);
        }
        return formattedDuration;
    }

    public void setFormattedDuration(String formattedDuration) {
        this.formattedDuration = formattedDuration;
    }

    public double getPassPercentage() {
        if (totalScenarios > 0) {
            return Math.round(((double) passedScenarios / totalScenarios) * 1000.0) / 10.0;
        }
        return 100.0;
    }

    public void setPassPercentage(double passPercentage) {
        this.passPercentage = passPercentage;
    }

    public List<String> getAllTags() {
        return allTags;
    }

    public void setAllTags(List<String> allTags) {
        this.allTags = allTags != null ? allTags : new ArrayList<>();
    }

    public int getTotalAttachments() {
        return totalAttachments;
    }

    public void setTotalAttachments(int totalAttachments) {
        this.totalAttachments = totalAttachments;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public static String formatDuration(long durationNanos) {
        if (durationNanos <= 0) {
            return "0ms";
        }
        long ms = durationNanos / 1_000_000L;
        if (ms < 1000) {
            return ms + "ms";
        }
        long seconds = ms / 1000L;
        long remainingMs = ms % 1000L;
        if (seconds < 60) {
            return seconds + "." + (remainingMs / 100) + "s";
        }
        long minutes = seconds / 60L;
        long remainingSeconds = seconds % 60L;
        if (minutes < 60) {
            return minutes + "m " + remainingSeconds + "s";
        }
        long hours = minutes / 60L;
        long remainingMinutes = minutes % 60L;
        return hours + "h " + remainingMinutes + "m";
    }
}
