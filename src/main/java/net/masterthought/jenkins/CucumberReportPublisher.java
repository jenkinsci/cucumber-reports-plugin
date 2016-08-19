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
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import javax.annotation.Nonnull;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.kohsuke.stapler.DataBoundConstructor;

import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import net.masterthought.cucumber.Reportable;

public class CucumberReportPublisher extends Publisher implements SimpleBuildStep {

    private final static String DEFAULT_FILE_INCLUDE_PATTERN = "**/*.json";

    private final static String REPORTS_DIR = "cucumber-reports";
    private final static String TRENDS_FILE = "cucumber-trends.json";

    public final String jsonReportDirectory;
    public final String fileIncludePattern;
    public final String fileExcludePattern;

    public final int failedStepsNumber;
    public final int skippedStepsNumber;
    public final int pendingStepsNumber;
    public final int undefinedStepsNumber;
    public final int failedScenariosNumber;
    public final int failedFeaturesNumber;
    public final Result buildStatus;

    public final boolean parallelTesting;
    public final String jenkinsBasePath;

    private File targetBuildDirectory;


    @DataBoundConstructor
    public CucumberReportPublisher(String jsonReportDirectory, String fileIncludePattern, String fileExcludePattern,
                                   int failedStepsNumber, int skippedStepsNumber, int pendingStepsNumber,
                                   int undefinedStepsNumber, int failedScenariosNumber, int failedFeaturesNumber,
                                   String buildStatus, boolean parallelTesting, String jenkinsBasePath) {

        this.jsonReportDirectory = jsonReportDirectory;
        this.fileIncludePattern = fileIncludePattern;
        this.fileExcludePattern = fileExcludePattern;
        this.failedStepsNumber = failedStepsNumber;
        this.skippedStepsNumber = skippedStepsNumber;
        this.pendingStepsNumber = pendingStepsNumber;
        this.undefinedStepsNumber = undefinedStepsNumber;
        this.failedScenariosNumber = failedScenariosNumber;
        this.failedFeaturesNumber = failedFeaturesNumber;
        this.buildStatus = buildStatus == null ? null : Result.fromString(buildStatus);
        this.parallelTesting = parallelTesting;
        this.jenkinsBasePath = jenkinsBasePath;
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
        log(listener, "Preparing Cucumber Reports");

        // create directory where trends will be stored
        final File trendsDir = new File(build.getParent().getRootDir(), REPORTS_DIR);
        if (!trendsDir.exists()) {
            if (!trendsDir.mkdir()) {
                throw new IllegalStateException("Could not create directory: " + trendsDir);
            }
        }

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

        int copiedFiles = workspaceJsonReportDirectory.copyRecursiveTo(DEFAULT_FILE_INCLUDE_PATTERN, new FilePath(targetBuildDirectory));
        log(listener, String.format("Copied %d json files from \"%s\" to reports directory \"%s\"",
                copiedFiles, workspaceJsonReportDirectory.getRemote(), targetBuildDirectory));

        // generate the reports from the targetBuildDirectory
        String[] jsonReportFiles = findJsonFiles(targetBuildDirectory, fileIncludePattern, fileExcludePattern);
        log(listener, String.format("Found %d json files.", jsonReportFiles.length));

        Configuration configuration = new Configuration(targetBuildDirectory, projectName);
        configuration.setParallelTesting(parallelTesting);
        configuration.setJenkinsBasePath(jenkinsBasePath);
        configuration.setRunWithJenkins(true);
        configuration.setBuildNumber(buildNumber);
        configuration.setTrendsStatsFile(new File(trendsDir, TRENDS_FILE));

        ReportBuilder reportBuilder = new ReportBuilder(
                fullPathToJsonFiles(jsonReportFiles, targetBuildDirectory), configuration);
        Reportable result = reportBuilder.generateReports();

        if (hasReportFailed(result, listener)) {
            // redefine build result if it was provided by plugin configuration
            if (buildStatus != null) {
                build.setResult(buildStatus);
            }
        }
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

    boolean hasReportFailed(Reportable result, TaskListener listener) {
        // happens when the resport could not be generated
        if (result == null) {
            log(listener, "Missing report result - report was not successfully completed");
            return true;
        }

        if (result.getFailedSteps() > failedStepsNumber) {
            log(listener, String.format("Found %d failed steps, while expected not more than %d",
                    result.getFailedSteps(), failedStepsNumber));
            return true;
        }
        if (result.getSkippedSteps() > skippedStepsNumber) {
            log(listener, String.format("Found %d skipped steps, while expected not more than %d",
                    result.getSkippedSteps(), skippedStepsNumber));
            return true;
        }
        if (result.getPendingSteps() > pendingStepsNumber) {
            log(listener, String.format("Found %d pending steps, while expected not more than %d",
                    result.getPendingSteps(), pendingStepsNumber));
            return true;
        }
        if (result.getUndefinedSteps() > undefinedStepsNumber) {
            log(listener, String.format("Found %d undefined steps, while expected not more than %d",
                    result.getUndefinedSteps(), undefinedStepsNumber));
            return true;
        }

        if (result.getFailedScenarios() > failedScenariosNumber) {
            log(listener, String.format("Found %d failed scenarios, while expected not more than %d",
                    result.getFailedScenarios(), failedScenariosNumber));
            return true;
        }
        if (result.getFailedFeatures() > failedFeaturesNumber) {
            log(listener, String.format("Found %d failed features, while expected not more than %d",
                    result.getFailedFeatures(), failedFeaturesNumber));
            return true;
        }

        return false;
    }

    private static void log(TaskListener listener, String message) {
        listener.getLogger().println("[CucumberReport] " + message);
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
    public static class DescriptorImpl extends CucumberReportBuildStepDescriptor {
    }
}
