package net.masterthought.jenkins;

import net.masterthought.jenkins.action.CucumberReportRunAction;

import java.io.File;

/**
 * This class implements a solution to serving various reports using JavaScript, Flash, etc. from Jenkins.
 * <p>
 * By default, Jenkins serves static files using a restrictive Content-Security-Policy header to prevent malicious users
 * from attacking other users of Jenkins by having Jenkins serve them maliciously manipulated files.
 * <p>
 * This presents an obstacle to plugins that wish to archive known safe reports in HTML format and have Jenkins serve them.
 * <p>
 * Examples include the Maven Site functionality in Maven Plugin, or the Javadoc Plugin.
 * <p>
 * This class implements a safe alternative to serving files from DirectoryBrowserSupport:
 * <p>
 * This action, when first attached, scans the specified directory and records all files' checksums.
 * <p>
 * When later asked to serve files, it compares the actual and expected checksums, and only serves matching files.
 *
 * @deprecated functionality moved to action package.
 */
@Deprecated
public class SafeArchiveServingAction extends CucumberReportRunAction {
    /**
     * Create a safe archive serving action.
     *
     * @param rootDir        The root directory to be served by this action
     * @param urlName        The URL name used for this action
     * @param indexFile      The file name of the index file to be served when accessing the urlName URL
     * @param iconName       The icon used for the action in the side panel
     * @param title          The title of this action in the side panel
     * @param safeExtensions The file extensions to be skipped from checksum recording and verification. These are file
     *                       types whose unauthorized modification does not constitute a risk to users when viewed in a
     *                       web browser. This should be resource file extensions like "gif" or "png" or file extensions
     *                       of files not viewed in a browser like "zip" or "gz". Never specify file types possibly
     *                       containing scripts or other possibly malicious data that can exploit users' browsers
     */
    SafeArchiveServingAction(File rootDir, String urlName, String indexFile, String iconName, String title, String... safeExtensions) {
        super(rootDir, urlName, indexFile, iconName, title, safeExtensions);
    }
}
