package pl.damianszczepanik.cucumber.reporting;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.FilePath;
import hudson.model.Action;
import hudson.model.DirectoryBrowserSupport;

public abstract class CucumberReportBaseAction implements Action {

    static final String BASE_URL = "cucumber-html-reports";
    private static final String NOFRAMES_INDEX = "feature-overview.html";

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
        DirectoryBrowserSupport dbs = new DirectoryBrowserSupport(this, new FilePath(dir()), getTitle(), getUrlName(),
                false);
        if (new File(dir(), NOFRAMES_INDEX).exists() && Boolean
                .valueOf(System.getProperty(CucumberReportBaseAction.class.getName() + ".useFramelessIndex", "true"))) {
            /* If an feature-overview.html exists, serve that, unless the system property evaluates to false */
            dbs.setIndexFileName(NOFRAMES_INDEX);
        }
        dbs.generateResponse(req, rsp, this);
    }

    protected abstract String getTitle();

    protected abstract File dir();
}
