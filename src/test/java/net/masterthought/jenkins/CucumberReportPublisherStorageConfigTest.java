package net.masterthought.jenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CucumberReportPublisherStorageConfigTest {

    @Test
    public void testStorageOptimizationDefaultsAndSetters() {
        CucumberReportPublisher publisher = new CucumberReportPublisher("**/cucumber.json");

        // Verify sensible defaults
        assertFalse(publisher.getEmbedFullAttachments(), "Default embedFullAttachments should be false");
        assertEquals(500, publisher.getAttachmentThresholdKB(), "Default threshold should be 500 KB");
        assertTrue(publisher.getCompressReport(), "Default compressReport should be true");

        // Verify setters
        publisher.setEmbedFullAttachments(true);
        assertTrue(publisher.getEmbedFullAttachments());

        publisher.setAttachmentThresholdKB(1024);
        assertEquals(1024, publisher.getAttachmentThresholdKB());

        publisher.setCompressReport(false);
        assertFalse(publisher.getCompressReport());
    }
}
