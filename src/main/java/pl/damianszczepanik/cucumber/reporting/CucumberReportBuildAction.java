package pl.damianszczepanik.cucumber.reporting;

import java.io.File;

import hudson.model.AbstractBuild;
import pl.damianszczepanik.cucumber.reporting.CucumberReportBaseAction;

public class CucumberReportBuildAction extends CucumberReportBaseAction {

    private final AbstractBuild<?, ?> build;

    public CucumberReportBuildAction(AbstractBuild<?, ?> build) {
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
