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

    static final String BASE_URL = "cucumber-html-reports";

    public String getUrlName() {
        return BASE_URL;
    }

    public String getDisplayName(){
        return "Cucumber Reports";
    }

    public String getIconFileName(){
            return "/plugin/cucumber-reports/cuke.png";
    }

    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        DirectoryBrowserSupport dbs = new DirectoryBrowserSupport(this, new FilePath(this.dir()), this.getTitle(), "graph.gif", false);
        dbs.setIndexFileName("feature-overview.html");
        dbs.generateResponse(req, rsp, this);
    }

    protected abstract String getTitle();

    protected abstract File dir();
}
