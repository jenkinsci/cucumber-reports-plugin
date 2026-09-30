package net.masterthought.jenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CucumberReportPublisherStorageConfigTest {

    @Test
    public void testStorageOptimizationConfigDefaultsAndSetters() {
        CucumberReportPublisher publisher = new CucumberReportPublisher("**/*.json");

        // Verify sensible defaults
        assertFalse(publisher.isEmbedFullAttachments(), "Default embedFullAttachments should be false");
        assertEquals(500, publisher.getAttachmentThresholdKB(), "Default threshold should be 500 KB");

        // Test mutating options
        publisher.setEmbedFullAttachments(true);
        assertTrue(publisher.isEmbedFullAttachments());

        publisher.setAttachmentThresholdKB(1024);
        assertEquals(1024, publisher.getAttachmentThresholdKB());

        // Test keepBackwardCompatibility reset of non-positive threshold
        publisher.setAttachmentThresholdKB(0);
        publisher.keepBackwardCompatibility();
        assertEquals(500, publisher.getAttachmentThresholdKB());

        publisher.setAttachmentThresholdKB(-100);
        publisher.keepBackwardCompatibility();
        assertEquals(500, publisher.getAttachmentThresholdKB());
    }
}
