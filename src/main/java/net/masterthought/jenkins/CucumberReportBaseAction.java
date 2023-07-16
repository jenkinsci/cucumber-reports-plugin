package net.masterthought.jenkins;

import hudson.model.Action;
import net.masterthought.cucumber.ReportBuilder;

public abstract class CucumberReportBaseAction implements Action {

    protected static final String ICON_NAME = "/plugin/cucumber-reports/icon.png";

    @Override
    public String getUrlName() {
        return ReportBuilder.HOME_PAGE;
    }

    @Override
    public String getIconFileName() {
        return ICON_NAME;
    }
}
