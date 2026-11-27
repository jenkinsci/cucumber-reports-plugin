package net.masterthought.jenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import hudson.Util;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    /**
     * The legacy checksum must reproduce exactly what cucumber-reports 5.11.0 stored, so it is compared with an
     * independent copy of the 5.11.0 loop kept in this test.
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 10, 1023, 1024, 1025, 2048, 3000})
    void legacyChecksumMatchesVersion5_11_0(int size) throws Exception {
        File file = writeFile("report-" + size + ".html", size);

        assertEquals(checksumAsRecordedBy5_11_0(file), SafeArchiveServingAction.calculateLegacyChecksum(file));
    }

    @Test
    void legacyAndCurrentChecksumsDifferWhenFinalBufferIsPartial() throws Exception {
        File file = writeFile("report.html", 1500);

        assertNotEquals(SafeArchiveServingAction.calculateChecksum(file),
                SafeArchiveServingAction.calculateLegacyChecksum(file));
    }

    @Test
    void actionFromOlderVersionAcceptsLegacyChecksum() throws Exception {
        File file = writeFile("report.html", 1500);
        SafeArchiveServingAction action = newAction();
        setChecksumFormat(action, SafeArchiveServingAction.CHECKSUM_FORMAT_UNSPECIFIED);

        assertTrue(action.matchesRecordedChecksum(file, checksumAsRecordedBy5_11_0(file)));
        assertTrue(action.matchesRecordedChecksum(file, SafeArchiveServingAction.calculateChecksum(file)));
    }

    @Test
    void actionFromOlderVersionRejectsModifiedFile() throws Exception {
        File file = writeFile("report.html", 1500);
        SafeArchiveServingAction action = newAction();
        setChecksumFormat(action, SafeArchiveServingAction.CHECKSUM_FORMAT_UNSPECIFIED);
        String recorded = checksumAsRecordedBy5_11_0(file);

        Files.writeString(file.toPath(), "<script>alert('tampered')</script>");

        assertFalse(action.matchesRecordedChecksum(file, recorded));
    }

    @Test
    void newActionRejectsLegacyChecksum() throws Exception {
        File file = writeFile("report.html", 1500);
        SafeArchiveServingAction action = newAction();

        assertTrue(action.matchesRecordedChecksum(file, SafeArchiveServingAction.calculateChecksum(file)));
        assertFalse(action.matchesRecordedChecksum(file, checksumAsRecordedBy5_11_0(file)));
    }

    private SafeArchiveServingAction newAction() {
        return new SafeArchiveServingAction(temporaryDirectory.toFile(), "cucumber-html-reports",
                "overview-features.html", "icon", "Cucumber reports");
    }

    private static void setChecksumFormat(SafeArchiveServingAction action, int format) throws Exception {
        Field field = SafeArchiveServingAction.class.getDeclaredField("checksumFormat");
        field.setAccessible(true);
        field.setInt(action, format);
    }

    private File writeFile(String name, int size) throws Exception {
        byte[] content = new byte[size];
        for (int i = 0; i < size; i++) {
            content[i] = (byte) (i * 31 + 7);
        }
        return Files.write(temporaryDirectory.resolve(name), content).toFile();
    }

    /** Copy of the checksum loop shipped in cucumber-reports 5.11.0. */
    static String checksumAsRecordedBy5_11_0(File file) throws Exception {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[1024];
            while (-1 != (fis.read(bytes))) {
                sha1.update(bytes);
            }
        }
        return Util.toHexString(sha1.digest());
    }
}
