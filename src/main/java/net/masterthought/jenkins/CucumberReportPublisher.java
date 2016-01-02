package net.masterthought.jenkins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.tools.ant.DirectoryScanner;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Result;
import hudson.slaves.SlaveComputer;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import net.masterthought.cucumber.ReportBuilder;

public class CucumberReportPublisher extends Recorder {

    private final static String DEFAULT_FILE_INCLUDE_PATTERN = "**/*.json";

    public final String jsonReportDirectory;
    public final String pluginUrlPath;
    public final String fileIncludePattern;
    public final String fileExcludePattern;
    public final boolean skippedFails;
    public final boolean pendingFails;
    public final boolean undefinedFails;
    public final boolean missingFails;
    public final boolean noFlashCharts;
    public final boolean ignoreFailedTests;
    public final boolean parallelTesting;

    @DataBoundConstructor
    public CucumberReportPublisher(String jsonReportDirectory, String pluginUrlPath, String fileIncludePattern, String fileExcludePattern, boolean skippedFails, boolean pendingFails, boolean undefinedFails, boolean missingFails, boolean noFlashCharts, boolean ignoreFailedTests, boolean parallelTesting) {
        this.jsonReportDirectory = jsonReportDirectory;
        this.pluginUrlPath = pluginUrlPath;
        this.fileIncludePattern = fileIncludePattern;
        this.fileExcludePattern = fileExcludePattern;

        this.skippedFails = skippedFails;
        this.pendingFails = pendingFails;
        this.undefinedFails = undefinedFails;
        this.missingFails = missingFails;

        this.noFlashCharts = noFlashCharts;
        this.ignoreFailedTests = ignoreFailedTests;
        this.parallelTesting = parallelTesting;
    }

    private String[] findJsonFiles(File targetDirectory, String fileIncludePattern, String fileExcludePattern) {
        DirectoryScanner scanner = new DirectoryScanner();
        if (fileIncludePattern == null || fileIncludePattern.isEmpty()) {
            scanner.setIncludes(new String[]{DEFAULT_FILE_INCLUDE_PATTERN});
        } else {
            scanner.setIncludes(new String[]{fileIncludePattern});
        }
        if (fileExcludePattern != null ) {
            scanner.setExcludes(new String[]{fileExcludePattern});
        }
        scanner.setBasedir(targetDirectory);
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {

        listener.getLogger().println("[CucumberReportPublisher] Compiling Cucumber Html Reports ...");

        // source directory (possibly on slave)
        FilePath workspaceJsonReportDirectory;
        if (jsonReportDirectory.isEmpty()) {
            workspaceJsonReportDirectory = build.getWorkspace();
        } else {
            workspaceJsonReportDirectory = new FilePath(build.getWorkspace(), jsonReportDirectory);
        }

        // target directory (always on master)
        File targetBuildDirectory = new File(build.getRootDir(), "cucumber-html-reports");
        if (!targetBuildDirectory.exists()) {
            targetBuildDirectory.mkdirs();
        }

        String buildNumber = Integer.toString(build.getNumber());
        String buildProject = build.getProject().getName();

        if (Computer.currentComputer() instanceof SlaveComputer) {
            listener.getLogger().println("[CucumberReportPublisher] Copying all json files from slave: " + workspaceJsonReportDirectory.getRemote() + " to master reports directory: " + targetBuildDirectory);
        } else {
            listener.getLogger().println("[CucumberReportPublisher] Copying all json files from: " + workspaceJsonReportDirectory.getRemote() + " to reports directory: " + targetBuildDirectory);
        }
        workspaceJsonReportDirectory.copyRecursiveTo(DEFAULT_FILE_INCLUDE_PATTERN, new FilePath(targetBuildDirectory));

        // generate the reports from the targetBuildDirectory
        Result result = Result.NOT_BUILT;
        String[] jsonReportFiles = findJsonFiles(targetBuildDirectory, fileIncludePattern, fileExcludePattern);
        if (jsonReportFiles.length > 0) {
            listener.getLogger().println(String.format("[CucumberReportPublisher] Found %d json files.", jsonReportFiles.length));
            int jsonIndex = 1;
            for (String jsonReportFile : jsonReportFiles) {
                listener.getLogger().println("[CucumberReportPublisher] " + jsonIndex + ". Found a json file: " + jsonReportFile);
                jsonIndex++;
            }
            listener.getLogger().println("[CucumberReportPublisher] Generating HTML reports");

            try {
                ReportBuilder reportBuilder = new ReportBuilder(
                        fullPathToJsonFiles(jsonReportFiles, targetBuildDirectory),
                        targetBuildDirectory,
                        pluginUrlPath,
                        buildNumber,
                        buildProject,
                        skippedFails,
                        pendingFails,
                        undefinedFails,
                        missingFails,
                        !noFlashCharts,
                        true,
                        false,
                        parallelTesting);
                reportBuilder.generateReports();

                if (reportBuilder.hasBuildPassed()) {
                    result = Result.SUCCESS;
                } else {
                    result = ignoreFailedTests ? Result.UNSTABLE : Result.FAILURE;
                }

            } catch (Exception e) {
                e.printStackTrace();
                result = Result.FAILURE;
                listener.getLogger().println("[CucumberReportPublisher] there was an error generating the reports: " + e);
                for(StackTraceElement error : e.getStackTrace()){
                   listener.getLogger().println(error);
                }
            }
        } else {
            result = Result.SUCCESS;
            listener.getLogger().println("[CucumberReportPublisher] there were no json results found in: " + targetBuildDirectory);
        }

        build.addAction(new CucumberReportBuildAction(build));
        build.setResult(result);

        return true;
    }

    private List<String> fullPathToJsonFiles(String[] jsonFiles, File targetBuildDirectory) {
        List<String> fullPathList = new ArrayList<String>();
        for (String file : jsonFiles) {
            fullPathList.add(new File(targetBuildDirectory, file).getAbsolutePath());
        }
        return fullPathList;
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return new CucumberReportProjectAction(project);
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
