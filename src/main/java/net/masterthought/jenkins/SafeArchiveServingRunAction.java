package net.masterthought.jenkins;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.model.Run;
import jenkins.model.RunAction2;

/**
 * Convenience implementation of {@link SafeArchiveServingAction} for runs that starts a scan on being attached to the
 * build.
 */
public class SafeArchiveServingRunAction extends SafeArchiveServingAction implements RunAction2 {

    public SafeArchiveServingRunAction(File rootDir, String urlName, String indexFile, String iconName, String title, String... safeExtensions) {
        super(rootDir, urlName, indexFile, iconName, title, safeExtensions);
    }

    @Override
    public void onAttached(Run<?, ?> r) {
        try {
            processDirectory();
        } catch (IOException | NoSuchAlgorithmException ex) {
            LOGGER.log(Level.WARNING, "Exception scanning " + r.getRootDir(), ex);
        }
    }

    @Override
    public void onLoad(Run<?, ?> r) {
    }

    private static final Logger LOGGER = Logger.getLogger(SafeArchiveServingRunAction.class.getName());
}
