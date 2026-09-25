package net.masterthought.jenkins.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AttachmentExternalizerTest {

    @TempDir
    Path tempDir;

    private AttachmentExternalizer externalizer;
    private File buildRootDir;

    @BeforeEach
    public void setUp() {
        buildRootDir = tempDir.resolve("build-test").toFile();
        buildRootDir.mkdirs();
        externalizer = new AttachmentExternalizer(buildRootDir);
    }

    @Test
    public void testSaveAndReadAttachment() throws Exception {
        String data = "Sample log output for cucumber step attachment";
        String fileId = externalizer.saveAttachment(data);

        assertNotNull(fileId);
        assertTrue(fileId.startsWith("att-"));
        assertTrue(fileId.endsWith(".dat"));

        String readData = externalizer.readAttachment(fileId);
        assertEquals(data, readData);
    }

    @Test
    public void testReadNonExistentAttachmentThrowsException() {
        assertThrows(IOException.class, () -> externalizer.readAttachment("att-nonexistent.dat"));
    }

    @Test
    public void testPathTraversalRejection() {
        assertThrows(SecurityException.class, () -> externalizer.getAttachmentFile("../secret.txt"));
        assertThrows(SecurityException.class, () -> externalizer.getAttachmentFile("/etc/passwd"));
        assertThrows(SecurityException.class, () -> externalizer.getAttachmentFile("sub/file.dat"));
        assertThrows(SecurityException.class, () -> externalizer.getAttachmentFile("sub\\file.dat"));
        assertThrows(SecurityException.class, () -> externalizer.getAttachmentFile(null));
    }

    @Test
    public void testGetAttachmentsDir() {
        File dir = externalizer.getAttachmentsDir();
        assertNotNull(dir);
        assertEquals(AttachmentExternalizer.ATTACHMENTS_DIR_NAME, dir.getName());
    }
}
