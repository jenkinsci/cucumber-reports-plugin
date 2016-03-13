package net.masterthought.jenkins;

import java.io.File;

import hudson.model.Run;

public class CucumberReportBuildAction extends CucumberReportBaseAction {

    private final Run<?, ?> build;

    public CucumberReportBuildAction(Run<?, ?> build) {
        this.build = build;
    }

    @Override
    protected String getTitle() {
        return this.build.getDisplayName();
    }

    @Override
    protected File dir() {
        return new File(build.getRootDir(), BASE_URL);
    }
}
