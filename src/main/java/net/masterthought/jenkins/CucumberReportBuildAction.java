package net.masterthought.jenkins;

import hudson.model.AbstractBuild;
import hudson.model.Run;

import java.io.File;

public class CucumberReportBuildAction extends CucumberReportBaseAction {
    private final AbstractBuild<?, ?> build;

    public CucumberReportBuildAction(AbstractBuild<?, ?> build) {
        super();
        this.build = build;
    }

    @Override
    protected String getTitle() {
        return this.build.getDisplayName() + " html3";
    }

    @Override
    protected File dir() {
        return new File(build.getRootDir(), "cucumber-html-reports");
    }

}
