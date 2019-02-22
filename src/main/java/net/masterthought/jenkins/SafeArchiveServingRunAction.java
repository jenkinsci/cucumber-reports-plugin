package net.masterthought.jenkins;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.model.Action;
import hudson.model.Run;
import javax.annotation.Nonnull;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;

/**
 * Convenience implementation of {@link SafeArchiveServingAction} for runs that starts a scan on being attached to the
 * build.
 */
public class SafeArchiveServingRunAction extends SafeArchiveServingAction implements RunAction2, SimpleBuildStep.LastBuildAction {

    private static final Logger LOGGER = Logger.getLogger(SafeArchiveServingRunAction.class.getName());

    private Run<?, ?> run;

	public SafeArchiveServingRunAction(@Nonnull Run<?, ?> r, File rootDir, String urlName, String indexFile, String iconName, String title, String... safeExtensions) {
        super(rootDir, urlName, indexFile, iconName, title, safeExtensions);
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
    	return Collections.singleton(new CucumberReportProjectAction(run.getParent()));
    }
}
