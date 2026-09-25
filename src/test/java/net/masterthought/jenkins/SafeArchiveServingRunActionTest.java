package net.masterthought.jenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hudson.model.Run;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Collections;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import net.masterthought.jenkins.storage.AttachmentExternalizer;
import net.masterthought.jenkins.storage.StorageOptimizer;
import net.masterthought.jenkins.storage.model.CucumberReportPayload;
import net.masterthought.jenkins.storage.model.ReportSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class SafeArchiveServingRunActionTest {

    @TempDir
    Path tempDir;

    private File buildRootDir;
    private Run<?, ?> run;
    private SafeArchiveServingRunAction action;
    private StorageOptimizer optimizer;

    @BeforeEach
    public void setUp() {
        buildRootDir = tempDir.resolve("build-test").toFile();
        buildRootDir.mkdirs();

        run = mock(Run.class);
        when(run.getRootDir()).thenReturn(buildRootDir);
        when(run.getNumber()).thenReturn(10);
        doNothing().when(run).checkPermission(any());

        optimizer = new StorageOptimizer(buildRootDir, false, 500);

        action = new SafeArchiveServingRunAction(
                run,
                buildRootDir,
                "cucumber-html-reports",
                "overview-features.html",
                "icon.png",
                "Cucumber Reports",
                ""
        );
    }

    @Test
    public void testGetPayloadAndSummaryWithSoftReference() throws Exception {
        assertNull(action.getPayload());
        assertNull(action.getSummary());

        CucumberReportPayload payload = new CucumberReportPayload();
        ReportSummary summary = new ReportSummary();
        summary.setTotalScenarios(5);
        summary.setPassedScenarios(4);
        summary.setFailedScenarios(1);
        payload.setSummary(summary);
        payload.setFeatures(Collections.emptyList());

        optimizer.savePayloadToGzip(payload);

        CucumberReportPayload loaded = action.getPayload();
        assertNotNull(loaded);
        assertEquals(5, loaded.getSummary().getTotalScenarios());

        // Second call should return the cached SoftReference payload
        CucumberReportPayload cached = action.getPayload();
        assertEquals(loaded, cached);

        ReportSummary actionSummary = action.getSummary();
        assertNotNull(actionSummary);
        assertEquals(5, actionSummary.getTotalScenarios());
    }

    @Test
    public void testDoDataEndpoint() throws Exception {
        CucumberReportPayload payload = new CucumberReportPayload();
        ReportSummary summary = new ReportSummary();
        summary.setTotalFeatures(2);
        payload.setSummary(summary);
        optimizer.savePayloadToGzip(payload);

        StaplerRequest req = mock(StaplerRequest.class);
        StaplerResponse rsp = mock(StaplerResponse.class);
        StringWriter sw = new StringWriter();
        when(rsp.getWriter()).thenReturn(new PrintWriter(sw));

        action.doData(req, rsp);

        verify(rsp).setContentType("application/json;charset=UTF-8");
        String output = sw.toString();
        assertTrue(output.contains("\"totalFeatures\":2"));
    }

    @Test
    public void testDoDownloadReportEndpoint() throws Exception {
        CucumberReportPayload payload = new CucumberReportPayload();
        optimizer.savePayloadToGzip(payload);

        StaplerRequest req = mock(StaplerRequest.class);
        StaplerResponse rsp = mock(StaplerResponse.class);
        StringWriter sw = new StringWriter();
        when(rsp.getWriter()).thenReturn(new PrintWriter(sw));

        action.doDownloadReport(req, rsp);

        verify(rsp).setContentType("application/json;charset=UTF-8");
        verify(rsp).setHeader("Content-Disposition", "attachment; filename=\"cucumber-report-10.json\"");
        assertTrue(sw.toString().length() > 0);
    }

    @Test
    public void testDoAttachmentSuccessAndDownload() throws Exception {
        AttachmentExternalizer ext = new AttachmentExternalizer(buildRootDir);
        String fileId = ext.saveAttachment("test image payload");

        StaplerRequest req = mock(StaplerRequest.class);
        StaplerResponse rsp = mock(StaplerResponse.class);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ServletOutputStream sos = new DelegatingServletOutputStream(baos);
        when(rsp.getOutputStream()).thenReturn(sos);

        action.doAttachment(fileId, "image/png", true, "screenshot.png", req, rsp);

        verify(rsp).setContentType("image/png");
        verify(rsp).setHeader("Content-Disposition", "attachment; filename=\"screenshot.png\"");
        assertEquals("test image payload", baos.toString("UTF-8"));
    }

    @Test
    public void testDoAttachmentMissingFileId() throws Exception {
        StaplerRequest req = mock(StaplerRequest.class);
        StaplerResponse rsp = mock(StaplerResponse.class);

        action.doAttachment("", "image/png", false, null, req, rsp);
        verify(rsp).sendError(400, "Missing fileId parameter");
    }

    @Test
    public void testDoAttachmentNotFound() throws Exception {
        StaplerRequest req = mock(StaplerRequest.class);
        StaplerResponse rsp = mock(StaplerResponse.class);

        action.doAttachment("att-missing.dat", "image/png", false, null, req, rsp);
        verify(rsp).sendError(404, "Attachment fragment not found");
    }

    @Test
    public void testDoAttachmentSecurityException() throws Exception {
        StaplerRequest req = mock(StaplerRequest.class);
        StaplerResponse rsp = mock(StaplerResponse.class);

        action.doAttachment("../secret.dat", "image/png", false, null, req, rsp);
        verify(rsp).sendError(anyInt(), anyString());
    }

    private static class DelegatingServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream baos;

        DelegatingServletOutputStream(ByteArrayOutputStream baos) {
            this.baos = baos;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
        }

        @Override
        public void write(int b) {
            baos.write(b);
        }
    }
}
