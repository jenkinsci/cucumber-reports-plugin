package net.masterthought.jenkins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CucumberReportPublisher extends Recorder {

    public final String jsonReportDirectory;
    public final String pluginUrlPath;
    public final boolean skippedFails;
    public final boolean undefinedFails;

    @DataBoundConstructor
    public CucumberReportPublisher(String jsonReportDirectory, String pluginUrlPath, boolean skippedFails, boolean undefinedFails) {
        this.jsonReportDirectory = jsonReportDirectory;
        this.pluginUrlPath = pluginUrlPath;
        this.skippedFails = skippedFails;
        this.undefinedFails = undefinedFails;
    }

    private String[] findJsonFiles(File targetDirectory) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setIncludes(new String[]{"**/*.json"});
        scanner.setBasedir(targetDirectory);
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        listener.getLogger().println("[CucumberReportPublisher] Compiling Cucumber Html Reports ...");

        File workspaceJsonReportDirectory = new File(build.getWorkspace().toURI().getPath(), jsonReportDirectory);
        File targetBuildDirectory = new File(build.getRootDir(), "cucumber-html-reports");

        String buildNumber = Integer.toString(build.getNumber());
        String buildProject = build.getProject().getName();

        if (!targetBuildDirectory.exists()) {
            targetBuildDirectory.mkdirs();
        }

        String[] files = findJsonFiles(workspaceJsonReportDirectory);

        boolean buildResult = true;

        if (files.length != 0) {
            listener.getLogger().println("[CucumberReportPublisher] copying json to reports directory: " + targetBuildDirectory);
            for (String file : files) {
                FileUtils.copyFile(new File(workspaceJsonReportDirectory.getPath() + "/" + file), new File(targetBuildDirectory, file));
            }

            String[] jsonReportFiles = findJsonFiles(targetBuildDirectory);
            listener.getLogger().println("[CucumberReportPublisher] Generating HTML reports");
            FeatureReportGenerator featureReportGenerator = new FeatureReportGenerator(fullPathToJsonFiles(jsonReportFiles, targetBuildDirectory), targetBuildDirectory, pluginUrlPath, buildNumber, buildProject, skippedFails, undefinedFails);
            try {
                featureReportGenerator.generateReports();
                buildResult = featureReportGenerator.getBuildStatus();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            listener.getLogger().println("[CucumberReportPublisher] here were no json results found in: " + workspaceJsonReportDirectory);
        }

        build.addAction(new CucumberReportBuildAction(build));
        return buildResult;
    }
    
    private List<String> fullPathToJsonFiles(String[] jsonFiles, File targetBuildDirectory){
        List<String> fullPathList = new ArrayList<String>();
        for(String file : jsonFiles){
         fullPathList.add(new File(targetBuildDirectory, file).getAbsolutePath());
      }
        return fullPathList;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        @Override
        public String getDisplayName() {
            return "Publish cucumber results as a report";
        }


        // Performs on-the-fly validation on the file mask wildcard.
        public FormValidation doCheck(@AncestorInPath AbstractProject project,
                                      @QueryParameter String value) throws IOException, ServletException {
            FilePath ws = project.getSomeWorkspace();
            return ws != null ? ws.validateRelativeDirectory(value) : FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }
}
