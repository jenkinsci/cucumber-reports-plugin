package net.masterthought.jenkins.storage.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a single BDD Step result within a Scenario.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StepResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String keyword;
    private String name;
    private int line;
    private String status; // passed, failed, skipped, pending, undefined
    private long duration; // in nanoseconds
    private String errorMessage;
    private String errorStackTrace;
    private String docString;
    private List<DataTableRow> rows = new ArrayList<>();
    private List<Attachment> attachments = new ArrayList<>();

    public StepResult() {
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getStatus() {
        return status != null ? status.toLowerCase(java.util.Locale.ROOT) : "unknown";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public double getDurationSeconds() {
        return duration / 1_000_000_000.0;
    }

    public boolean isDurationWarning() {
        return getDurationSeconds() >= 5.0;
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

    public String getDocString() {
        return docString;
    }

    public void setDocString(String docString) {
        this.docString = docString;
    }

    public List<DataTableRow> getRows() {
        return rows;
    }

    public void setRows(List<DataTableRow> rows) {
        this.rows = rows != null ? rows : new ArrayList<>();
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments != null ? attachments : new ArrayList<>();
    }
}
