package net.masterthought.jenkins;

import hudson.Util;
import hudson.model.Action;
import hudson.util.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements a solution to serving various reports using JavaScript, Flash, etc. from Jenkins.
 *
 * By default, Jenkins serves static files using a restrictive Content-Security-Policy header to prevent malicious users
 * from attacking other users of Jenkins by having Jenkins serve them maliciously manipulated files.
 *
 * This presents an obstacle to plugins that wish to archive known safe reports in HTML format and have Jenkins serve them.
 *
 * Examples include the Maven Site functionality in Maven Plugin, or the Javadoc Plugin.
 *
 * This class implements a safe alternative to serving files from DirectoryBrowserSupport:
 *
 * This action, when first attached, scans the specified directory and records all files' checksums.
 *
 * When later asked to serve files, it compares the actual and expected checksums, and only serves matching files.
 *
 */
public class SafeArchiveServingAction implements Action {

    private Map<String,String> fileChecksums = new HashMap<>();

    private final File rootDir;

    private final String urlName;

    private final String indexFile;

    private final String iconName;

    private final String title;

    private final List<String> safeExtensions;

    /**
     * Create a safe archive serving action.
     *
     * @param rootDir The root directory to be served by this action
     * @param urlName The URL name used for this action
     * @param indexFile The file name of the index file to be served when accessing the urlName URL
     * @param iconName The icon used for the action in the side panel
     * @param title The title of this action in the side panel
     * @param safeExtensions The file extensions to be skipped from checksum recording and verification. These are file
     *                       types whose unauthorized modification does not constitute a risk to users when viewed in a
     *                       web browser. This should be resource file extensions like "gif" or "png" or file extensions
     *                       of files not viewed in a browser like "zip" or "gz". Never specify file types possibly
     *                       containing scripts or other possibly malicious data that can exploit users' browsers
     *                       (html, js, swf, css, …).
     */
    public SafeArchiveServingAction(File rootDir, String urlName, String indexFile, String iconName, String title, String... safeExtensions) {
        this.rootDir = rootDir;
        this.urlName = urlName;
        this.indexFile = indexFile;
        this.iconName = iconName;
        this.title = title;
        this.safeExtensions = Collections.unmodifiableList(Arrays.asList(safeExtensions));
    }

    private void addFile(String relativePath, String checksum) {
        this.fileChecksums.put(relativePath, checksum);
    }

    private String getChecksum(String file) {
        if (file == null || !fileChecksums.containsKey(file)) {
            throw new IllegalArgumentException(file + " has no checksum recorded");
        }
        return fileChecksums.get(file);
    }

    private String calculateChecksum(@Nonnull File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[1024];
            while (-1 != (fis.read(bytes))) {
                sha1.update(bytes);
            }
        }
        return Util.toHexString(sha1.digest());
    }

    private void processDirectory(@Nonnull File directory, @Nullable String path) throws NoSuchAlgorithmException, IOException {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.log(Level.FINER, "Scanning " + getRootDir());
        }
        File[] files = directory.listFiles();
        if (files == null) {
            throw new IllegalArgumentException(directory + " listing returned null");
        }
        for (File file : files) {

            String relativePath = file.getName();
            if (path != null) {
                relativePath = path + "/" + relativePath;
            }

            if (file.isDirectory()) {
                processDirectory(file, relativePath);
            }
            if (file.isFile() && !isSafeFileType(file.getName())) {
                addFile(relativePath, calculateChecksum(file));
            }
        }
    }

    /**
     * Record the checksums of files in the specified directory and its descendants unless a file type is whitelisted as
     * safe.
     *
     * @throws NoSuchAlgorithmException If the platform does unexpectedly not support SHA-1
     * @throws IOException
     */
    public void processDirectory() throws NoSuchAlgorithmException, IOException {
        LOGGER.log(Level.FINE, "Scanning " + getRootDir());
        processDirectory(getRootDir(), null);
    }


    private boolean isSafeFileType(String filename) {
        for (String extension : this.safeExtensions) {
            if (filename.endsWith("." + extension)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getIconFileName() {
        return iconName;
    }

    @Override
    public String getDisplayName() {
        return title;
    }

    @Override
    public String getUrlName() {
        return urlName;
    }

    public File getRootDir() {
        return rootDir;
    }

    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Serving " + req.getRestOfPath());
        }
        if (req.getRestOfPath().equals("")) {
            // serve the index page
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Redirecting to index file");
            }
            throw HttpResponses.redirectTo(indexFile);
        }

        String fileName = req.getRestOfPath();
        if (fileName.startsWith("/")) {
            fileName = fileName.substring(1);
        }

        File file = new File(getRootDir(), fileName);

        if (!new File(getRootDir(), fileName).exists()) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "File does not exist: " + fileName);
            }

            throw HttpResponses.notFound();
        }

        if (isSafeFileType(fileName)) {
            // skip checksum check if the file's extension is whitelisted
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Serving safe file: " + fileName);
            }

            serveFile(file, req, rsp);
            return;
        }

        // if we're here, we know it's not a safe file type based on name

        if (!fileChecksums.containsKey(fileName)) {
            // file had no checksum recorded -- dangerous
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "File exists but no checksum recorded: " + fileName);
            }

            throw HttpResponses.notFound();
        }

        // checksum recorded

        // do not serve files outside the archive directory
        if (!file.getAbsolutePath().startsWith(this.getRootDir().getAbsolutePath())) {
            // TODO symlinks and similar insanity?
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "File is outside archive directory: " + fileName);
            }

            throw HttpResponses.notFound();
        }

        // calculate actual file checksum
        String actualChecksum;
        try {
            actualChecksum = calculateChecksum(file);
        } catch (NoSuchAlgorithmException nse) {
            // cannot happen
            throw new IllegalStateException(nse);
        }

        String expectedChecksum = getChecksum(fileName);

        if (!expectedChecksum.equals(actualChecksum)) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Checksum mismatch: recorded: " +
                        expectedChecksum + ", actual: " + actualChecksum + " for file: " + fileName);
            }

            throw HttpResponses.forbidden();
        }

        serveFile(file, req, rsp);

    }

    private void serveFile(File file, StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        // serve the file without Content-Security-Policy
        long lastModified = file.lastModified();
        long length = file.length();
        try (InputStream in = new FileInputStream(file)) {
            rsp.serveFile(req, in, lastModified, -1, length, file.getName());
        }
    }

    private static final Logger LOGGER = Logger.getLogger(SafeArchiveServingAction.class.getName());
}
