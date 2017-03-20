package net.masterthought.jenkins.action;

import hudson.model.Action;
import hudson.model.ProminentProjectAction;
import net.masterthought.jenkins.Messages;

public class CucumberReportProjectAction implements ProminentProjectAction, Action {

    static final String ICON_NAME = "/plugin/cucumber-reports/icon.png";
    private String url;

    CucumberReportProjectAction(String url) {
        this.url = url;
    }

    @Override
    public String getUrlName() {
        return url;
    }

    @Override
    public String getIconFileName() {
        return ICON_NAME;
    }

    @Override
    public String getDisplayName() {
        return Messages.Plugin_DisplayName();
    }
}
