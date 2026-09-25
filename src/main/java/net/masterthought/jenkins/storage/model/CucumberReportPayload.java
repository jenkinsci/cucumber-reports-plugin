package net.masterthought.jenkins.storage.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Top-level DTO representing the entire processed BDD report.
 * This object is serialized to GZIP on the controller disk.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CucumberReportPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private ReportSummary summary = new ReportSummary();
    private List<FeatureResult> features = new ArrayList<>();
    private boolean embedFullAttachments;
    private int attachmentThresholdKB;
    private String buildDisplayName;
    private String buildUrl;

    public CucumberReportPayload() {
    }

    public ReportSummary getSummary() {
        return summary;
    }

    public void setSummary(ReportSummary summary) {
        this.summary = summary;
    }

    public List<FeatureResult> getFeatures() {
        return features;
    }

    public void setFeatures(List<FeatureResult> features) {
        this.features = features != null ? features : new ArrayList<>();
    }

    public boolean isEmbedFullAttachments() {
        return embedFullAttachments;
    }

    public void setEmbedFullAttachments(boolean embedFullAttachments) {
        this.embedFullAttachments = embedFullAttachments;
    }

    public int getAttachmentThresholdKB() {
        return attachmentThresholdKB;
    }

    public void setAttachmentThresholdKB(int attachmentThresholdKB) {
        this.attachmentThresholdKB = attachmentThresholdKB;
    }

    public String getBuildDisplayName() {
        return buildDisplayName;
    }

    public void setBuildDisplayName(String buildDisplayName) {
        this.buildDisplayName = buildDisplayName;
    }

    public String getBuildUrl() {
        return buildUrl;
    }

    public void setBuildUrl(String buildUrl) {
        this.buildUrl = buildUrl;
    }
}
