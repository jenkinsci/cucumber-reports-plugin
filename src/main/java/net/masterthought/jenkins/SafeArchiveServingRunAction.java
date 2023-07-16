package net.masterthought.jenkins;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Action;
import hudson.model.Run;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;

/**
 * Convenience implementation of {@link SafeArchiveServingAction} for runs that starts a scan on being attached to the
 * build.
 */
public class SafeArchiveServingRunAction extends SafeArchiveServingAction implements RunAction2, SimpleBuildStep.LastBuildAction {

    private static final Logger LOGGER = Logger.getLogger(SafeArchiveServingRunAction.class.getName());

    private Run<?, ?> run;
    private String directorySuffix;

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

    @Override
    public Collection<? extends Action> getProjectActions() {
    	return Collections.singleton(new CucumberReportProjectAction(run.getParent(), getDisplayName(), directorySuffix));
    }
}
