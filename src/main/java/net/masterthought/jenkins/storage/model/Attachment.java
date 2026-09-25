package net.masterthought.jenkins.storage.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

/**
 * Model representing an embedding or attachment attached to a Cucumber Step or Hook.
 * Supports externalization to fragment files for controller disk optimization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Attachment implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String mimeType;
    private String data;
    private boolean isExternal;
    private String externalFileId;
    private long sizeBytes;
    private boolean truncated;
    private String preview;

    public Attachment() {
    }

    public Attachment(String name, String mimeType, String data) {
        this.name = name;
        this.mimeType = mimeType;
        this.data = data;
        if (data != null) {
            this.sizeBytes = data.length();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean isExternal() {
        return isExternal;
    }

    public void setExternal(boolean external) {
        isExternal = external;
    }

    public String getExternalFileId() {
        return externalFileId;
    }

    public void setExternalFileId(String externalFileId) {
        this.externalFileId = externalFileId;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }
}
