package net.masterthought.jenkins;

import hudson.model.Action;

import net.masterthought.cucumber.ReportBuilder;

public abstract class CucumberReportBaseAction implements Action {

    protected static final String ICON_NAME = "/plugin/cucumber-reports/icon.png";

    private static final String DEFAULT_PAGE = ReportBuilder.HOME_PAGE;

    @Override
    public String getUrlName() {
        return ReportBuilder.HOME_PAGE;
    }

    @Override
    public String getIconFileName() {
        return ICON_NAME;
    }

    @Override
    public String getDisplayName() {
        return Messages.Configuration_DisplayName();
    }
}
