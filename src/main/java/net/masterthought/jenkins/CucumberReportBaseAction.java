package net.masterthought.jenkins;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.FilePath;
import hudson.model.Action;
import hudson.model.DirectoryBrowserSupport;

public abstract class CucumberReportBaseAction implements Action {

    protected static final String BASE_URL = "cucumber-html-reports";

    private static final String DEFAULT_PAGE = "feature-overview.html";

    public String getUrlName() {
        return BASE_URL;
    }

    public String getDisplayName() {
        return Messages.SidePanel_DisplayName();
    }

    public String getIconFileName() {
        return "/plugin/cucumber-reports/cuke.png";
    }

    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        // since Jenkins blocks JavaScript as described at
        // https://wiki.jenkins-ci.org/display/JENKINS/Configuring+Content+Security+Policy and fact that plugin uses JS
        // to display charts, following must be applied
        System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "");

        DirectoryBrowserSupport dbs = new DirectoryBrowserSupport(this, new FilePath(dir()), getTitle(), getUrlName(),
                false);

        dbs.setIndexFileName(DEFAULT_PAGE);
        dbs.generateResponse(req, rsp, this);
    }

    protected abstract String getTitle();

    protected abstract File dir();
}
