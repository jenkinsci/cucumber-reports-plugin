package net.masterthought.jenkins;

import hudson.model.Job;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;

import net.masterthought.cucumber.ReportBuilder;

public class CucumberReportProjectAction extends CucumberReportBaseAction implements ProminentProjectAction {

    private final Job<?, ?> project;
    private String reportTitle;
    private String directoryQualifier;

    public CucumberReportProjectAction(Job<?, ?> project, String reportTitle, String directoryQualifier) {
        this.reportTitle = reportTitle;
        this.project = project;
        this.directoryQualifier = directoryQualifier;
    }

    @Override
    public String getUrlName() {
        Run<?, ?> run = this.project.getLastCompletedBuild();
        if (run != null) {
            return run.getNumber() + "/" + ReportBuilder.BASE_DIRECTORY + directoryQualifier + "/" + ReportBuilder.HOME_PAGE;
        }

        // none build was completed, report is yet not available
        return "";
    }

    @Override
    public String getDisplayName() {
        return reportTitle;
    }
}