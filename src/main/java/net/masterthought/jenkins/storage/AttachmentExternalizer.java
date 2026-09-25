package net.masterthought.jenkins.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

/**
 * Utility responsible for writing and reading large attachment fragment files to and from disk.
 * Protects against directory traversal and ensures safe controller disk I/O.
 */
public class AttachmentExternalizer {

    private static final Logger LOGGER = Logger.getLogger(AttachmentExternalizer.class.getName());
    public static final String ATTACHMENTS_DIR_NAME = "cucumber-attachments";

    private final File attachmentsDir;

    public AttachmentExternalizer(File buildRootDir) {
        this.attachmentsDir = new File(buildRootDir, ATTACHMENTS_DIR_NAME);
    }

    /**
     * Saves raw attachment data to an external fragment file.
     *
     * @param data Raw base64 or string data
     * @return Unique filename of the saved fragment
     * @throws IOException If disk write fails
     */
    public String saveAttachment(String data) throws IOException {
        if (!attachmentsDir.exists() && !attachmentsDir.mkdirs()) {
            throw new IOException("Failed to create attachments directory: " + attachmentsDir.getAbsolutePath());
        }

        String fileId = "att-" + UUID.randomUUID().toString() + ".dat";
        File targetFile = new File(attachmentsDir, fileId);

        try (OutputStream os = new FileOutputStream(targetFile)) {
            if (data != null) {
                os.write(data.getBytes(StandardCharsets.UTF_8));
            }
        }
        return fileId;
    }

    /**
     * Reads the external attachment fragment from disk safely.
     *
     * @param fileId Unique filename of the fragment
     * @return Raw content as String
     * @throws IOException If file does not exist or cannot be read
     */
    public String readAttachment(String fileId) throws IOException {
        File file = getAttachmentFile(fileId);
        if (!file.exists() || !file.isFile()) {
            throw new IOException("Attachment fragment not found: " + fileId);
        }

        try (InputStream is = new FileInputStream(file)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }

    /**
     * Resolves the fragment file ensuring no path traversal escapes the attachments directory.
     */
    public File getAttachmentFile(String fileId) throws IOException {
        if (fileId == null || fileId.contains("..") || fileId.contains("/") || fileId.contains("\\")) {
            throw new SecurityException("Invalid attachment file identifier: " + fileId);
        }
        File resolved = new File(attachmentsDir, fileId);
        if (!resolved.getCanonicalPath().startsWith(attachmentsDir.getCanonicalPath())) {
            throw new SecurityException("Directory traversal attempt detected: " + fileId);
        }
        return resolved;
    }

    public File getAttachmentsDir() {
        return attachmentsDir;
    }
}
