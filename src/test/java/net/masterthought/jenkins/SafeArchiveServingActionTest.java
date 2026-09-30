package net.masterthought.jenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import hudson.Util;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SafeArchiveServingActionTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void calculatesChecksumForPartialFinalBuffer() throws Exception {
        byte[] content = new byte[1025];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) i;
        }
        Path file = Files.write(temporaryDirectory.resolve("report.html"), content);

        String expected = Util.toHexString(MessageDigest.getInstance("SHA-1").digest(content));

        assertEquals(expected, SafeArchiveServingAction.calculateChecksum(file.toFile()));
    }
}
