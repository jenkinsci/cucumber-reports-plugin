package net.masterthought.jenkins;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import javax.annotation.Nonnull;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import net.masterthought.cucumber.Reportable;

public class CucumberReportPublisher extends Publisher implements SimpleBuildStep {

    private final static String DEFAULT_FILE_INCLUDE_PATTERN = "**/*.json";

    private final static String TRENDS_DIR = "cucumber-reports";
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
    public final String buildStatus;

    private int trendsLimit;
    private boolean parallelTesting;
    private List<Classification> classifications = Collections.emptyList();

    @DataBoundConstructor
    public CucumberReportPublisher(String jsonReportDirectory, String fileIncludePattern, String fileExcludePattern,
                                   int failedStepsNumber, int skippedStepsNumber, int pendingStepsNumber,
                                   int undefinedStepsNumber, int failedScenariosNumber, int failedFeaturesNumber,
                                   String buildStatus) {

        this.jsonReportDirectory = jsonReportDirectory;
        this.fileIncludePattern = fileIncludePattern;
        this.fileExcludePattern = fileExcludePattern;
        this.failedStepsNumber = failedStepsNumber;
        this.skippedStepsNumber = skippedStepsNumber;
        this.pendingStepsNumber = pendingStepsNumber;
        this.undefinedStepsNumber = undefinedStepsNumber;
        this.failedScenariosNumber = failedScenariosNumber;
        this.failedFeaturesNumber = failedFeaturesNumber;

        this.buildStatus = buildStatus;
    }

    @DataBoundSetter
    public void setClassifications(List<Classification> classifications) {
        // don't store the classifications if there was no element provided
        if (CollectionUtils.isNotEmpty(classifications)) {
            this.classifications = classifications;
        }
    }

    public List<Classification> getClassifications() {
        return classifications;
    }

    @DataBoundSetter
    public void setTrendsLimit(int trendsLimit) {
        this.trendsLimit = trendsLimit;
    }

    public int getTrendsLimit() {
        return trendsLimit;
    }

    @DataBoundSetter
    public void setParallelTesting(boolean parallelTesting) {
        this.parallelTesting = parallelTesting;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {

        generateReport(run, workspace, listener);

        SafeArchiveServingRunAction caa = new SafeArchiveServingRunAction(new File(run.getRootDir(), ReportBuilder.BASE_DIRECTORY),
                ReportBuilder.BASE_DIRECTORY, ReportBuilder.HOME_PAGE, CucumberReportBaseAction.ICON_NAME, Messages.SidePanel_DisplayName());
        run.addAction(caa);
    }

    private void generateReport(Run<?, ?> build, FilePath workspace, TaskListener listener) throws InterruptedException, IOException {
        log(listener, "Preparing Cucumber Reports");

        // create directory where trends will be stored
        final File trendsDir = new File(build.getParent().getRootDir(), TRENDS_DIR);
        if (!trendsDir.exists()) {
            if (!trendsDir.mkdir()) {
                throw new IllegalStateException("Could not create directory for trends: " + trendsDir);
            }
        }

        // source directory (possibly on slave)
        FilePath inputDirectory = new FilePath(workspace, jsonReportDirectory);

        File directoryForReport = build.getRootDir();
        File directoryJsonCache = new File(directoryForReport, ReportBuilder.BASE_DIRECTORY + File.separatorChar + ".cache");
        if (directoryJsonCache.mkdir()) {
            throw new IllegalStateException("Could not create directory for cache: " + directoryJsonCache);
        }
        int copiedFiles = inputDirectory.copyRecursiveTo(DEFAULT_FILE_INCLUDE_PATTERN, new FilePath(directoryJsonCache));
        log(listener, String.format("Copied %d json files from workspace \"%s\" to reports directory \"%s\"",
                copiedFiles, inputDirectory.getRemote(), directoryJsonCache));

        // exclude JSONs that should be skipped (as configured by the user)
        String[] jsonReportFiles = findJsonFiles(directoryJsonCache, fileIncludePattern, fileExcludePattern);
        List<String> jsonFilesToProcess = fullPathToJsonFiles(jsonReportFiles, directoryJsonCache);
        log(listener, String.format("Processing %d json files:", jsonReportFiles.length));
        for (String jsonFile : jsonFilesToProcess) {
            log(listener, jsonFile);
        }

        String buildNumber = Integer.toString(build.getNumber());
        // this works for normal and multi-config/matrix jobs
        // for matrix jobs, this will include the matrix job name and the specific
        // configuration/permutation name as well. this also includes the '/' so
        // we don't have to modify how the cucumber plugin report generator's links
        String projectName = build.getParent().getDisplayName();

        Configuration configuration = new Configuration(directoryForReport, projectName);
        configuration.setParallelTesting(parallelTesting);
        configuration.setRunWithJenkins(true);
        configuration.setBuildNumber(buildNumber);
        configuration.setTrends(new File(trendsDir, TRENDS_FILE), trendsLimit);
        if (CollectionUtils.isNotEmpty(classifications)) {
            log(listener, String.format("%d classifications to be added in the report", classifications.size()));
            for (Classification classification : classifications) {
                log(listener, String.format("Adding classification - %s:%s", classification.key, classification.value));
                configuration.addClassifications(classification.key,
                        evaluateMacro(build, listener, classification.value));
            }
        }

        ReportBuilder reportBuilder = new ReportBuilder(jsonFilesToProcess, configuration);
        Reportable result = reportBuilder.generateReports();

        if (hasReportFailed(result, listener)) {
            // redefine build result if it was provided by plugin configuration
            if (buildStatus != null) {
                log(listener, "Build status is changed to " + buildStatus.toString());
                build.setResult(Result.fromString(buildStatus));
            } else {
                log(listener, "Build status is left unchanged");
            }
        }
    }

    private String[] findJsonFiles(File targetDirectory, String fileIncludePattern, String fileExcludePattern) {
        DirectoryScanner scanner = new DirectoryScanner();

        if (StringUtils.isEmpty(fileIncludePattern)) {
            scanner.setIncludes(new String[]{DEFAULT_FILE_INCLUDE_PATTERN});
        } else {
            scanner.setIncludes(new String[]{fileIncludePattern});
        }
        if (StringUtils.isNotEmpty(fileExcludePattern)) {
            scanner.setExcludes(new String[]{fileExcludePattern});
        }
        scanner.setBasedir(targetDirectory);
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    private List<String> fullPathToJsonFiles(String[] jsonFiles, File targetBuildDirectory) {
        List<String> fullPathList = new ArrayList<>();
        for (String file : jsonFiles) {
            fullPathList.add(new File(targetBuildDirectory, file).getAbsolutePath());
        }
        return fullPathList;
    }

    private boolean hasReportFailed(Reportable result, TaskListener listener) {
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

    private String evaluateMacro(Run<?, ?> build, TaskListener listener, String value) throws InterruptedException, IOException {
        try {
            return TokenMacro.expandAll((AbstractBuild) build, listener, value);
        } catch (MacroEvaluationException e) {
            log(listener, String.format("Could not evaluate macro '%s': %s", value, e.getMessage()));
            return value;
        } catch (ClassCastException e) {
            log(listener, String.format("ClassCastException '%s': %s", value, e.getMessage()));
            return value;
        }
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

    public static class Classification extends AbstractDescribableImpl<Classification> implements Serializable {

        public String key;
        public String value;

        @DataBoundConstructor
        public Classification(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<Classification> {

            @Override
            public String getDisplayName() {
                return "";
            }
        }
    }

    @Extension
    public static class DescriptorImpl extends CucumberReportBuildStepDescriptor {
    }
}
