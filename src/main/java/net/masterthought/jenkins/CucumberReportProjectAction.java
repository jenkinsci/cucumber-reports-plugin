package net.masterthought.jenkins;

import hudson.model.Job;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;

import net.masterthought.cucumber.ReportBuilder;

public class CucumberReportProjectAction extends CucumberReportBaseAction implements ProminentProjectAction {

    private final Job<?, ?> project;

    public CucumberReportProjectAction(Job<?, ?> project) {
        this.project = project;
    }

    @Override
    public String getUrlName() {
        Run<?, ?> run = this.project.getLastCompletedBuild();
        if (run != null) {
            return run.getNumber() + "/" + ReportBuilder.BASE_DIRECTORY + "/" + ReportBuilder.HOME_PAGE;
        }

        // none build was completed, report is yet not available
        return "";
    }
}