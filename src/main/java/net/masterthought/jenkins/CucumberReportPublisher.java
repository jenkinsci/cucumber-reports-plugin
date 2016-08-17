package net.masterthought.jenkins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Computer;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.slaves.SlaveComputer;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import javax.annotation.Nonnull;
import jenkins.tasks.SimpleBuildStep;
import org.apache.tools.ant.DirectoryScanner;
import org.kohsuke.stapler.DataBoundConstructor;

import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;

public class CucumberReportPublisher extends Publisher implements SimpleBuildStep {

    private final static String DEFAULT_FILE_INCLUDE_PATTERN = "**/*.json";

    public final String jsonReportDirectory;
    public final String jenkinsBasePath;
    public final String fileIncludePattern;
    public final String fileExcludePattern;
    public final boolean skippedFails;
    public final boolean pendingFails;
    public final boolean undefinedFails;
    public final boolean missingFails;
    public final boolean ignoreFailedTests;
    public final boolean parallelTesting;

    private File targetBuildDirectory;

    @DataBoundConstructor
    public CucumberReportPublisher(String jsonReportDirectory, String jenkinsBasePath, String fileIncludePattern,
                                   String fileExcludePattern, boolean skippedFails, boolean pendingFails, boolean undefinedFails,
                                   boolean missingFails, boolean ignoreFailedTests, boolean parallelTesting) {
        this.jsonReportDirectory = jsonReportDirectory;
        this.jenkinsBasePath = jenkinsBasePath;
        this.fileIncludePattern = fileIncludePattern;
        this.fileExcludePattern = fileExcludePattern;

        this.skippedFails = skippedFails;
        this.pendingFails = pendingFails;
        this.undefinedFails = undefinedFails;
        this.missingFails = missingFails;

        this.ignoreFailedTests = ignoreFailedTests;
        this.parallelTesting = parallelTesting;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {

        targetBuildDirectory = run.getRootDir();

        generateReport(run, workspace, listener);

        SafeArchiveServingRunAction caa = new SafeArchiveServingRunAction(new File(run.getRootDir(), ReportBuilder.BASE_DIRECTORY),
                ReportBuilder.BASE_DIRECTORY, ReportBuilder.HOME_PAGE, CucumberReportBaseAction.ICON_NAME, Messages.SidePanel_DisplayName());
        run.addAction(caa);
    }


    private void generateReport(Run<?, ?> build, FilePath workspace, TaskListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("[CucumberReportPublisher] Compiling Cucumber Reports ...");

        // source directory (possibly on slave)
        FilePath workspaceJsonReportDirectory;
        if (jsonReportDirectory.isEmpty()) {
            workspaceJsonReportDirectory = workspace;
        } else {
            workspaceJsonReportDirectory = new FilePath(workspace, jsonReportDirectory);
        }

        String buildNumber = Integer.toString(build.getNumber());
        // this works for normal and multi-config/matrix jobs
        // for matrix jobs, this will include the matrix job name and the specific
        // configuration/permutation name as well. this also includes the '/' so
        // we don't have to modify how the cucumber plugin report generator's links
        String projectName = build.getParent().getDisplayName();

        if (Computer.currentComputer() instanceof SlaveComputer) {
            listener.getLogger().println("[CucumberReportPublisher] Copying all json files from slave: " + workspaceJsonReportDirectory.getRemote() + " to master reports directory: " + targetBuildDirectory);
        } else {
            listener.getLogger().println("[CucumberReportPublisher] Copying all json files from: " + workspaceJsonReportDirectory.getRemote() + " to reports directory: " + targetBuildDirectory);
        }
        workspaceJsonReportDirectory.copyRecursiveTo(DEFAULT_FILE_INCLUDE_PATTERN, new FilePath(targetBuildDirectory));

        // generate the reports from the targetBuildDirectory
        Result result;
        String[] jsonReportFiles = findJsonFiles(targetBuildDirectory, fileIncludePattern, fileExcludePattern);
        listener.getLogger().println(String.format("[CucumberReportPublisher] Found %d json files.", jsonReportFiles.length));

        try {
            Configuration configuration = new Configuration(targetBuildDirectory, projectName);
            configuration.setStatusFlags(skippedFails, pendingFails, undefinedFails, missingFails);
            configuration.setParallelTesting(parallelTesting);
            configuration.setJenkinsBasePath(jenkinsBasePath);
            configuration.setRunWithJenkins(true);
            configuration.setBuildNumber(buildNumber);

            ReportBuilder reportBuilder = new ReportBuilder(
                    fullPathToJsonFiles(jsonReportFiles, targetBuildDirectory), configuration);
            reportBuilder.generateReports();

            if (reportBuilder.hasBuildPassed()) {
                result = Result.SUCCESS;
            } else {
                result = ignoreFailedTests ? Result.UNSTABLE : Result.FAILURE;
            }

        } catch (Exception e) {
            result = Result.FAILURE;
            listener.getLogger().println("[CucumberReportPublisher] there was an error generating the reports: " + e);
            for (StackTraceElement error : e.getStackTrace()) {
                listener.getLogger().println(error);
            }
        }

        build.setResult(result);
    }

    private String[] findJsonFiles(File targetDirectory, String fileIncludePattern, String fileExcludePattern) {
        DirectoryScanner scanner = new DirectoryScanner();

        if (fileIncludePattern == null || fileIncludePattern.isEmpty()) {
            scanner.setIncludes(new String[]{DEFAULT_FILE_INCLUDE_PATTERN});
        } else {
            scanner.setIncludes(new String[]{fileIncludePattern});
        }
        if (fileExcludePattern != null) {
            scanner.setExcludes(new String[]{fileExcludePattern});
        }
        scanner.setBasedir(targetDirectory);
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    private List<String> fullPathToJsonFiles(String[] jsonFiles, File targetBuildDirectory) {
        List<String> fullPathList = new ArrayList<String>();
        for (String file : jsonFiles) {
            fullPathList.add(new File(targetBuildDirectory, file).getAbsolutePath());
        }
        return fullPathList;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return new CucumberReportProjectAction(project);
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        @Override
        public String getDisplayName() {
            return Messages.Configuration_DisplayName();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
