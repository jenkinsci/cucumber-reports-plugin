package net.masterthought.jenkins;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Action;
import hudson.model.Item;
import hudson.model.Run;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;
import net.masterthought.jenkins.storage.AttachmentExternalizer;
import net.masterthought.jenkins.storage.StorageOptimizer;
import net.masterthought.jenkins.storage.model.CucumberReportPayload;
import net.masterthought.jenkins.storage.model.ReportSummary;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Convenience implementation of {@link SafeArchiveServingAction} for runs that starts a scan on being attached to the
 * build. Provides lazy loading for compressed BDD test reports and endpoints for attachment fragments.
 */
public class SafeArchiveServingRunAction extends SafeArchiveServingAction implements RunAction2, SimpleBuildStep.LastBuildAction {

    private static final Logger LOGGER = Logger.getLogger(SafeArchiveServingRunAction.class.getName());

    private Run<?, ?> run;
    private String directorySuffix;

    /**
     * SoftReference memory cache for the report payload.
     * Allows the JVM Garbage Collector to reclaim heap memory whenever memory is constrained,
     * ensuring zero memory leakage on the Jenkins Controller.
     */
    private transient SoftReference<CucumberReportPayload> payloadRef;

    public SafeArchiveServingRunAction(@NonNull Run<?, ?> r, File rootDir, String urlName, String indexFile, String iconName, String title, String directorySuffix, String... safeExtensions) {
        super(rootDir, urlName, indexFile, iconName, title, safeExtensions);
        this.directorySuffix = directorySuffix;
        this.run = r;
    }

    @Override
    public void onAttached(Run<?, ?> r) {
        try {
            this.run = r;
            processDirectory();
        } catch (IOException | NoSuchAlgorithmException ex) {
            LOGGER.log(Level.WARNING, "Exception scanning " + r.getRootDir(), ex);
        }
    }

    @Override
    public void onLoad(Run<?, ?> r) {
        this.run = r;
    }

    public Run<?, ?> getRun() {
        return run;
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return Collections.singleton(new CucumberReportProjectAction(run.getParent(), getDisplayName(), directorySuffix));
    }

    /**
     * Retrieves the report payload lazily from disk or from SoftReference cache.
     */
    public synchronized CucumberReportPayload getPayload() {
        if (payloadRef != null) {
            CucumberReportPayload payload = payloadRef.get();
            if (payload != null) {
                return payload;
            }
        }
        if (run != null) {
            try {
                CucumberReportPayload payload = StorageOptimizer.loadPayloadFromGzip(run.getRootDir());
                if (payload != null) {
                    payloadRef = new SoftReference<>(payload);
                    return payload;
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load compressed cucumber report payload from gzip", e);
            }
        }
        return null;
    }

    public ReportSummary getSummary() {
        CucumberReportPayload payload = getPayload();
        return payload != null ? payload.getSummary() : null;
    }

    /**
     * Stapler Web Endpoint: Serves or downloads an externalized attachment fragment.
     * URL: buildUrl/cucumber-html-reports/attachment?fileId=att-xxx.dat&mimeType=image/png&download=true
     */
    public void doAttachment(@QueryParameter(required = true) String fileId,
                             @QueryParameter String mimeType,
                             @QueryParameter boolean download,
                             @QueryParameter String name,
                             StaplerRequest req,
                             StaplerResponse rsp) throws IOException {
        if (run != null) {
            run.checkPermission(Item.READ);
        }

        if (fileId == null || fileId.trim().isEmpty()) {
            rsp.sendError(400, "Missing fileId parameter");
            return;
        }

        if (run == null) {
            rsp.sendError(404, "Run not attached");
            return;
        }

        AttachmentExternalizer externalizer = new AttachmentExternalizer(run.getRootDir());
        File attachmentFile;
        try {
            attachmentFile = externalizer.getAttachmentFile(fileId);
        } catch (SecurityException se) {
            rsp.sendError(403, "Access denied: " + se.getMessage());
            return;
        }

        if (!attachmentFile.exists() || !attachmentFile.isFile()) {
            rsp.sendError(404, "Attachment fragment not found");
            return;
        }

        String contentType = (mimeType != null && !mimeType.trim().isEmpty()) ? mimeType : "text/plain";
        rsp.setContentType(contentType);

        if (download) {
            String downloadName = (name != null && !name.trim().isEmpty()) ? name : fileId;
            String encodedName = URLEncoder.encode(downloadName, StandardCharsets.UTF_8.name()).replace("+", "%20");
            rsp.setHeader("Content-Disposition", "attachment; filename=\"" + encodedName + "\"");
        }

        try (InputStream is = new FileInputStream(attachmentFile);
             OutputStream os = rsp.getOutputStream()) {
            IOUtils.copy(is, os);
        }
    }

    /**
     * Stapler Web Endpoint: Returns raw decompressed report JSON.
     * URL: buildUrl/cucumber-html-reports/data
     */
    public void doData(StaplerRequest req, StaplerResponse rsp) throws IOException {
        if (run != null) {
            run.checkPermission(Item.READ);
        }

        rsp.setContentType("application/json;charset=UTF-8");
        rsp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        if (run == null) {
            rsp.getWriter().write("{}");
            return;
        }

        String json = StorageOptimizer.readRawJsonFromGzip(run.getRootDir());
        rsp.getWriter().write(json);
    }

    /**
     * Stapler Web Endpoint: Allows downloading full decompressed report JSON file.
     * URL: buildUrl/cucumber-html-reports/downloadReport
     */
    public void doDownloadReport(StaplerRequest req, StaplerResponse rsp) throws IOException {
        if (run != null) {
            run.checkPermission(Item.READ);
        }

        rsp.setContentType("application/json;charset=UTF-8");
        rsp.setHeader("Content-Disposition", "attachment; filename=\"cucumber-report-" + (run != null ? run.getNumber() : "export") + ".json\"");

        if (run != null) {
            String json = StorageOptimizer.readRawJsonFromGzip(run.getRootDir());
            rsp.getWriter().write(json);
        } else {
            rsp.getWriter().write("{}");
        }
    }
}
