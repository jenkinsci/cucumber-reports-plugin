package net.masterthought.jenkins;

import java.io.File;

import hudson.model.AbstractItem;
import hudson.model.AbstractProject;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;

public class CucumberReportProjectAction extends CucumberReportBaseAction implements ProminentProjectAction {

    private final AbstractProject<?, ?> project;

    public CucumberReportProjectAction(AbstractItem project) {
        this.project = (AbstractProject) project;
    }

    @Override
    protected File dir() {
        Run<?, ?> run = this.project.getLastCompletedBuild();
        if (run != null) {
            File archiveDir = getBuildArchiveDir(run);

            if (archiveDir.exists()) {
                return archiveDir;
            }
        }

        return getProjectArchiveDir(this.project);
    }

    private File getProjectArchiveDir(AbstractItem project) {
        return new File(project.getRootDir(), CucumberReportBaseAction.BASE_URL);
    }

    /** Gets the directory where the HTML report is stored for the given build. */
    private File getBuildArchiveDir(Run<?, ?> run) {
        return new File(run.getRootDir(), CucumberReportBaseAction.BASE_URL);
    }

    @Override
    protected String getTitle() {
        return this.project.getDisplayName();
    }
}