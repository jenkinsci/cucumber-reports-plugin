package net.masterthought.jenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import org.htmlunit.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Reports recorded by cucumber-reports 5.11.0 and older must still be served after upgrading, without weakening the
 * tamper check for those reports or for newly recorded ones.
 */
@WithJenkins
@Issue("570")
class LegacyChecksumIntegrationTest {

    private static final String URL_NAME = "cucumber-html-reports";
    private static final String REPORT = "overview-features.html";

    private JenkinsRule j;
    private FreeStyleBuild build;
    private File reportDir;
    private File report;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;
        FreeStyleProject project = j.createFreeStyleProject();
        build = j.buildAndAssertSuccess(project);
        reportDir = new File(build.getRootDir(), URL_NAME);
        assertTrue(reportDir.mkdirs());
        // 1,500 bytes: the final 1,024-byte read is partial, so the 5.11.0 and current checksums differ
        report = new File(reportDir, REPORT);
        Files.write(report.toPath(), htmlOfLength(1500));
    }

    @Test
    void reportRecordedBy5_11_0IsServed() throws Exception {
        attach(persistedByOlderVersion(SafeArchiveServingActionTest.checksumAsRecordedBy5_11_0(report)));

        assertEquals(200, statusOf(REPORT));
    }

    @Test
    void reportRecordedBy5_12_0IsServed() throws Exception {
        attach(persistedByOlderVersion(SafeArchiveServingAction.calculateChecksum(report)));

        assertEquals(200, statusOf(REPORT));
    }

    @Test
    void tamperedReportRecordedBy5_11_0IsRejected() throws Exception {
        attach(persistedByOlderVersion(SafeArchiveServingActionTest.checksumAsRecordedBy5_11_0(report)));

        Files.writeString(report.toPath(), "<html><script>alert('tampered')</script></html>");

        assertEquals(403, statusOf(REPORT));
    }

    @Test
    void newReportIsServedAndTamperingIsRejected() throws Exception {
        SafeArchiveServingAction action = newAction();
        action.processDirectory();
        attach(action);
        assertEquals(200, statusOf(REPORT));

        Files.writeString(report.toPath(), "<html><script>alert('tampered')</script></html>");

        assertEquals(403, statusOf(REPORT));
    }

    @Test
    void newReportDoesNotAcceptLegacyChecksum() throws Exception {
        SafeArchiveServingAction action = newAction();
        action.processDirectory();
        String xml = Run.XSTREAM2.toXML(action).replace(
                SafeArchiveServingAction.calculateChecksum(report),
                SafeArchiveServingActionTest.checksumAsRecordedBy5_11_0(report));
        attach((SafeArchiveServingAction) Run.XSTREAM2.fromXML(xml));

        assertEquals(403, statusOf(REPORT));
    }

    @Test
    void checksumFormatSurvivesSaving() throws Exception {
        SafeArchiveServingAction action = newAction();
        action.processDirectory();

        String xml = Run.XSTREAM2.toXML(action);

        assertTrue(xml.contains("<checksumFormat>" + SafeArchiveServingAction.CHECKSUM_FORMAT_SHA1 + "</checksumFormat>"), xml);
        attach((SafeArchiveServingAction) Run.XSTREAM2.fromXML(xml));
        assertEquals(200, statusOf(REPORT));
    }

    private SafeArchiveServingAction newAction() {
        return new SafeArchiveServingAction(reportDir, URL_NAME, REPORT, "icon", "Cucumber reports",
                "png", "jpg", "gif", "svg");
    }

    /**
     * Simulates a build.xml entry written by a plugin version without the checksumFormat field: the field is
     * removed from the XML and the recorded checksum replaced, then the action is loaded back through XStream as
     * Jenkins does when it loads a build.
     */
    private SafeArchiveServingAction persistedByOlderVersion(String recordedChecksum) throws Exception {
        SafeArchiveServingAction current = newAction();
        current.processDirectory();
        String xml = Run.XSTREAM2.toXML(current);
        assertTrue(xml.contains("<checksumFormat>"), xml);
        String oldXml = xml
                .replaceAll("\\s*<checksumFormat>\\d+</checksumFormat>", "")
                .replace(SafeArchiveServingAction.calculateChecksum(report), recordedChecksum);
        assertFalse(oldXml.contains("checksumFormat"), oldXml);
        return (SafeArchiveServingAction) Run.XSTREAM2.fromXML(oldXml);
    }

    private void attach(SafeArchiveServingAction action) throws Exception {
        build.addAction(action);
        build.save();
    }

    private int statusOf(String file) throws Exception {
        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc.getOptions().setJavaScriptEnabled(false);
            Page page = wc.goTo(build.getUrl() + URL_NAME + "/" + file, null);
            return page.getWebResponse().getStatusCode();
        }
    }

    private static byte[] htmlOfLength(int length) {
        StringBuilder html = new StringBuilder("<html><body>");
        while (html.length() < length - "</body></html>".length()) {
            html.append('x');
        }
        html.append("</body></html>");
        return html.toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }
}
