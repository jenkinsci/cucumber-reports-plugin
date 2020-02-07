package net.masterthought.jenkins;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import javax.annotation.Nonnull;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import net.masterthought.cucumber.Reportable;
import net.masterthought.cucumber.presentation.PresentationMode;
import net.masterthought.cucumber.reducers.ReducingMethod;
import net.masterthought.cucumber.sorting.SortingMethod;

public class CucumberReportPublisher extends Publisher implements SimpleBuildStep {

    private final static String DEFAULT_FILE_INCLUDE_PATTERN_JSONS = "**/*.json";
    private final static String DEFAULT_FILE_INCLUDE_PATTERN_CLASSIFICATIONS = "**/*.properties";

    private final static String TRENDS_DIR = "cucumber-reports";
    private final static String TRENDS_FILE = "cucumber-trends.json";

    private final String fileIncludePattern;
    private String fileExcludePattern = "";
    private String jsonReportDirectory = "";
    private String reportTitle = "";
    private String directoryQualifier = "";

    private int failedStepsNumber;
    private int skippedStepsNumber;
    private int pendingStepsNumber;
    private int undefinedStepsNumber;
    private int failedScenariosNumber;
    private int failedFeaturesNumber;

    private double failedStepsPercentage;
    private double skippedStepsPercentage;
    private double pendingStepsPercentage;
    private double undefinedStepsPercentage;
    private double failedScenariosPercentage;
    private double failedFeaturesPercentage;

    private String buildStatus;
    private boolean stopBuildOnFailedReport;

    private int trendsLimit;
    private String sortingMethod;
    private List<Classification> classifications;

    private boolean mergeFeaturesById;
    private boolean mergeFeaturesWithRetest;
    private boolean hideEmptyHooks;
    private boolean skipEmptyJSONFiles;
    private boolean expandAllSteps;

    private String classificationsFilePattern = "";

    @DataBoundConstructor
    public CucumberReportPublisher(String fileIncludePattern) {
        this.fileIncludePattern = fileIncludePattern;
    }

    /**
     * This method, invoked after object is resurrected from persistence,
     * to keep backward compatibility.
     */
    protected void keepBackwardCompatibility() {
        if (classifications == null) {
            classifications = new LinkedList<>();
        }
        if (sortingMethod == null) {
            sortingMethod = SortingMethod.NATURAL.name();
        }
        
        reportTitle = StringUtils.defaultString(reportTitle);
        directoryQualifier = StringUtils.defaultString(directoryQualifier);
    }

    private static void log(TaskListener listener, String message) {
        listener.getLogger().println("[CucumberReport] " + message);
    }

    public String getFileIncludePattern() {
        return fileIncludePattern;
    }

    public List<Classification> getClassifications() {
        return classifications;
    }

    @DataBoundSetter
    public void setClassifications(List<Classification> classifications) {
        // don't store the classifications if there was no element provided
        if (CollectionUtils.isNotEmpty(classifications)) {
            this.classifications = classifications;
        }
    }

    public int getTrendsLimit() {
        return trendsLimit;
    }

    @DataBoundSetter
    public void setTrendsLimit(int trendsLimit) {
        this.trendsLimit = trendsLimit;
    }

    public String getFileExcludePattern() {
        return fileExcludePattern;
    }

    @DataBoundSetter
    public void setFileExcludePattern(String fileExcludePattern) {
        this.fileExcludePattern = fileExcludePattern;
    }

    public String getJsonReportDirectory() {
        return jsonReportDirectory;
    }

    @DataBoundSetter
    public void setJsonReportDirectory(String jsonReportDirectory) {
        this.jsonReportDirectory = jsonReportDirectory;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    @DataBoundSetter
    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
        this.directoryQualifier = "_" +
                (
                        StringUtils.isEmpty(reportTitle)
                                ? ""
                                : UUID.nameUUIDFromBytes(reportTitle.getBytes(StandardCharsets.UTF_8)).toString()
                );
    }

    public int getFailedStepsNumber() {
        return failedStepsNumber;
    }

    @DataBoundSetter
    public void setFailedStepsNumber(int failedStepsNumber) {
        this.failedStepsNumber = failedStepsNumber;
    }

    public int getSkippedStepsNumber() {
        return skippedStepsNumber;
    }

    @DataBoundSetter
    public void setSkippedStepsNumber(int skippedStepsNumber) {
        this.skippedStepsNumber = skippedStepsNumber;
    }

    public int getPendingStepsNumber() {
        return pendingStepsNumber;
    }

    @DataBoundSetter
    public void setPendingStepsNumber(int pendingStepsNumber) {
        this.pendingStepsNumber = pendingStepsNumber;
    }

    public int getUndefinedStepsNumber() {
        return undefinedStepsNumber;
    }

    @DataBoundSetter
    public void setUndefinedStepsNumber(int undefinedStepsNumber) {
        this.undefinedStepsNumber = undefinedStepsNumber;
    }

    public int getFailedScenariosNumber() {
        return failedScenariosNumber;
    }

    @DataBoundSetter
    public void setFailedScenariosNumber(int failedScenariosNumber) {
        this.failedScenariosNumber = failedScenariosNumber;
    }

    public int getFailedFeaturesNumber() {
        return failedFeaturesNumber;
    }

    @DataBoundSetter
    public void setFailedFeaturesNumber(int failedFeaturesNumber) {
        this.failedFeaturesNumber = failedFeaturesNumber;
    }


    public double getFailedStepsPercentage() {
        return failedStepsPercentage;
    }

    @DataBoundSetter
    public void setFailedStepsPercentage(double failedStepsPercentage) {
        this.failedStepsPercentage = failedStepsPercentage;
    }

    public double getSkippedStepsPercentage() {
        return skippedStepsPercentage;
    }

    @DataBoundSetter
    public void setSkippedStepsPercentage(double skippedStepsPercentage) {
        this.skippedStepsPercentage = skippedStepsPercentage;
    }

    public double getPendingStepsPercentage() {
        return pendingStepsPercentage;
    }

    @DataBoundSetter
    public void setPendingStepsPercentage(double pendingStepsPercentage) {
        this.pendingStepsPercentage = pendingStepsPercentage;
    }

    public double getUndefinedStepsPercentage() {
        return undefinedStepsPercentage;
    }

    @DataBoundSetter
    public void setUndefinedStepsPercentage(double undefinedStepsPercentage) {
        this.undefinedStepsPercentage = undefinedStepsPercentage;
    }

    public double getFailedScenariosPercentage() {
        return failedScenariosPercentage;
    }

    @DataBoundSetter
    public void setFailedScenariosPercentage(double failedScenariosPercentage) {
        this.failedScenariosPercentage = failedScenariosPercentage;
    }

    public double getFailedFeaturesPercentage() {
        return failedFeaturesPercentage;
    }

    @DataBoundSetter
    public void setFailedFeaturesPercentage(double failedFeaturesPercentage) {
        this.failedFeaturesPercentage = failedFeaturesPercentage;
    }


    public String getBuildStatus() {
        return buildStatus;
    }

    @DataBoundSetter
    public void setBuildStatus(String buildStatus) {
        this.buildStatus = buildStatus;
    }

    @DataBoundSetter
    public void setStopBuildOnFailedReport(boolean stopBuildOnFailedReport) {
        this.stopBuildOnFailedReport = stopBuildOnFailedReport;
    }

    public boolean getStopBuildOnFailedReport() {
        return stopBuildOnFailedReport;
    }

    @DataBoundSetter
    public void setSortingMethod(String sortingMethod) {
        this.sortingMethod = sortingMethod;
    }

    public String getSortingMethod() {
        return sortingMethod;
    }

    @DataBoundSetter
    public void setClassificationsFilePattern(String classificationsFilePattern) {
        this.classificationsFilePattern = classificationsFilePattern;
    }

    public String getClassificationsFilePattern() {
        return classificationsFilePattern;
    }

    @DataBoundSetter
    public void setMergeFeaturesById(boolean mergeFeaturesById) {
        this.mergeFeaturesById = mergeFeaturesById;
    }

    public boolean getMergeFeaturesById() {
        return mergeFeaturesById;
    }

    @DataBoundSetter
    public void setMergeFeaturesWithRetest(boolean mergeFeaturesWithRetest) {
        this.mergeFeaturesWithRetest = mergeFeaturesWithRetest;
    }

    public boolean getMergeFeaturesWithRetest() {
        return mergeFeaturesWithRetest;
    }

    @DataBoundSetter
    public void setHideEmptyHooks(boolean hideEmptyHooks) {
        this.hideEmptyHooks = hideEmptyHooks;
    }

    public boolean getHideEmptyHooks() {
        return hideEmptyHooks;
    }

    @DataBoundSetter
    public void setSkipEmptyJSONFiles(boolean skipEmptyJSONFiles) {
        this.skipEmptyJSONFiles = skipEmptyJSONFiles;
    }

    public boolean getSkipEmptyJSONFiles() {
        return skipEmptyJSONFiles;
    }

    @DataBoundSetter
    public void setExpandAllSteps(boolean expandAllSteps) {
        this.expandAllSteps = expandAllSteps;
    }

    public boolean getExpandAllSteps() {
        return expandAllSteps;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {

        keepBackwardCompatibility();
        if (StringUtils.isNotEmpty(reportTitle)) {

            classifications.add(new Classification(Messages.Classification_ReportTitle(), reportTitle));
        }

        generateReport(run, workspace, listener);

        SafeArchiveServingRunAction caa = new SafeArchiveServingRunAction(
                run,
                new File(run.getRootDir(), ReportBuilder.BASE_DIRECTORY + this.directoryQualifier),
                ReportBuilder.BASE_DIRECTORY + this.directoryQualifier,
                ReportBuilder.HOME_PAGE,
                CucumberReportBaseAction.ICON_NAME,
                getActionName(),
                directoryQualifier
        );
        run.addAction(caa);
    }

    private String getActionName() {
        return StringUtils.isEmpty(reportTitle) ? Messages.SidePanel_DisplayNameNoTitle() : String.format(Messages.SidePanel_DisplayName(), reportTitle);
    }

    private void generateReport(Run<?, ?> build, FilePath workspace, TaskListener listener) throws InterruptedException, IOException {

        log(listener, "Using Cucumber Reports version " + getPomVersion(listener));

        // create directory where trends will be stored
        final File trendsDir = new File(build.getParent().getRootDir(), TRENDS_DIR + directoryQualifier);
        if (!trendsDir.exists() && !trendsDir.mkdirs()) {
            throw new IllegalStateException("Could not create directory for trends: " + trendsDir);
        }

        // source directory (possibly on slave)
        String parsedJsonReportDirectory = evaluateMacro(build, workspace, listener, jsonReportDirectory);
        log(listener, String.format("JSON report directory is \"%s\"", parsedJsonReportDirectory));
        FilePath inputDirectory = new FilePath(workspace, parsedJsonReportDirectory);

        File directoryForReport = new File(build.getRootDir(), ReportBuilder.BASE_DIRECTORY + directoryQualifier);
        File directoryJsonCache = new File(directoryForReport, ".cache");
        if (!directoryJsonCache.exists() && !directoryJsonCache.mkdirs()) {
            throw new IllegalStateException("Could not create directory for cache: " + directoryJsonCache);
        }
        // copies JSON files to cache...
        int copiedFiles = inputDirectory.copyRecursiveTo(fileIncludePattern, new FilePath(directoryJsonCache));
        log(listener, String.format("Copied %d json files from workspace \"%s\" to reports directory \"%s\"",
                copiedFiles, inputDirectory.getRemote(), directoryJsonCache));
        // copies Classifications files to cache...
        int copiedFilesProperties = inputDirectory.copyRecursiveTo(DEFAULT_FILE_INCLUDE_PATTERN_CLASSIFICATIONS, new FilePath(directoryJsonCache));
        log(listener, String.format("Copied %d properties files from workspace \"%s\" to reports directory \"%s\"",
                copiedFilesProperties, inputDirectory.getRemote(), directoryJsonCache));

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
        configuration.setBuildNumber(buildNumber);
        configuration.setTrends(new File(trendsDir, TRENDS_FILE), trendsLimit);
        configuration.setSortingMethod(SortingMethod.valueOf(sortingMethod));
        if (mergeFeaturesById) {
            configuration.addReducingMethod(ReducingMethod.MERGE_FEATURES_BY_ID);
        }
        if (mergeFeaturesWithRetest) {
            configuration.addReducingMethod(ReducingMethod.MERGE_FEATURES_WITH_RETEST);
        }
        if (skipEmptyJSONFiles) {
            configuration.addReducingMethod(ReducingMethod.SKIP_EMPTY_JSON_FILES);
        }
        if (hideEmptyHooks) {
            configuration.addReducingMethod(ReducingMethod.HIDE_EMPTY_HOOKS);
        }
        if (expandAllSteps) {
            configuration.addPresentationModes(PresentationMode.EXPAND_ALL_STEPS);
        }

        configuration.addPresentationModes(PresentationMode.RUN_WITH_JENKINS);

        if (CollectionUtils.isNotEmpty(classifications)) {
            log(listener, String.format("Adding %d classification(s)", classifications.size()));
            addClassificationsToBuildReport(build, workspace, listener, configuration, classifications);
        }

        List<String> classificationFiles = fetchPropertyFiles(directoryJsonCache, listener);
        if(CollectionUtils.isNotEmpty(classificationFiles)) {
            configuration.addClassificationFiles(classificationFiles);
        }

        ReportBuilder reportBuilder = new ReportBuilder(jsonFilesToProcess, configuration);
        Reportable result = reportBuilder.generateReports();

        if (hasReportFailed(result, listener)) {
            // redefine build result if it was provided by plugin configuration
            if (buildStatus != null) {
                log(listener, "Build status is changed to " + buildStatus);
                build.setResult(Result.fromString(buildStatus));
            } else {
                log(listener, "Build status is left unchanged");
            }

            if (stopBuildOnFailedReport) {
                throw new AbortException(Messages.StopBuildOnFailedReport_FailNote());
            }
        }

        // removes cache which may run out of the free space on storage
        FileUtils.deleteQuietly(directoryJsonCache);

        //Once created, the report will be in jobs/JobName/xxx/cucumber-reports_UUID/cucumber_reports
        //This is a way to work around hardcoded paths in cucumber report builder plugin
        //We need to move these into  jobs/JobName/xxx/cucumber-reports_UUID
        moveFilesToPermamentLocation(directoryForReport, ReportBuilder.BASE_DIRECTORY);
    }

    private void moveFilesToPermamentLocation(File directoryForReport, String subdirectory) throws IOException {
        // reportFilesLocation will be jobs/JobName/xxx/cucumber-reports_UUID/cucumber_reports
        File reportFilesLocation = new File(directoryForReport, subdirectory);
        // get everything from there and move into jobs/JobName/xxx/cucumber-reports_UUID
        File[] reportFiles = reportFilesLocation.listFiles();
        if (reportFiles != null) {
            for (File f : reportFiles) {
                FileUtils.moveToDirectory(f, directoryForReport, false);
            }
        }
        // and then delete empty directory jobs/JobName/xxx/cucumber-reports_UUID/cucumber-reports
        FileUtils.deleteQuietly(reportFilesLocation);
    }

    private String getPomVersion(TaskListener listener) {
        Properties properties = new Properties();
        try (InputStream inputStream = this.getClass().getClassLoader()
                .getResourceAsStream("plugin.properties")) {
            properties.load(inputStream);
            return properties.getProperty("plugin.version");
        } catch (IOException e) {
            log(listener, e.getMessage());
            return "";
        }
    }

    private String[] findJsonFiles(File targetDirectory, String fileIncludePattern, String fileExcludePattern) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(targetDirectory);

        if (StringUtils.isEmpty(fileIncludePattern)) {
            scanner.setIncludes(new String[]{DEFAULT_FILE_INCLUDE_PATTERN_JSONS});
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
        // happens when the report could not be generated
        if (result == null) {
            log(listener, "Missing report result - report was not successfully completed");
            return true;
        }

        if (failedStepsNumber != -1 && result.getFailedSteps() > failedStepsNumber) {
            log(listener, String.format("Found %d failed steps, while expected at most %d",
                    result.getFailedSteps(), failedStepsNumber));
            return true;
        }
        if (skippedStepsNumber != -1 && result.getSkippedSteps() > skippedStepsNumber) {
            log(listener, String.format("Found %d skipped steps, while expected at most %d",
                    result.getSkippedSteps(), skippedStepsNumber));
            return true;
        }
        if (pendingStepsNumber != -1 && result.getPendingSteps() > pendingStepsNumber) {
            log(listener, String.format("Found %d pending steps, while expected at most %d",
                    result.getPendingSteps(), pendingStepsNumber));
            return true;
        }
        if (undefinedStepsNumber != -1 && result.getUndefinedSteps() > undefinedStepsNumber) {
            log(listener, String.format("Found %d undefined steps, while expected at most %d",
                    result.getUndefinedSteps(), undefinedStepsNumber));
            return true;
        }
        if (failedScenariosNumber != -1 && result.getFailedScenarios() > failedScenariosNumber) {
            log(listener, String.format("Found %d failed scenarios, while expected at most %d",
                    result.getFailedScenarios(), failedScenariosNumber));
            return true;
        }
        if (failedFeaturesNumber != -1 && result.getFailedFeatures() > failedFeaturesNumber) {
            log(listener, String.format("Found %d failed features, while expected at most %d",
                    result.getFailedFeatures(), failedFeaturesNumber));
            return true;
        }

        double failedStepsThreshold = 100.0 * result.getFailedSteps() / result.getSteps();
        if (failedStepsThreshold > failedStepsPercentage) {
            log(listener, String.format("Found %f failed steps, while expected not more than %f percent",
                    failedStepsThreshold, failedStepsPercentage));
            return true;
        }
        double skippedStepsThreshold = 100.0 * result.getSkippedSteps() / result.getSteps();
        if (skippedStepsThreshold > skippedStepsPercentage) {
            log(listener, String.format("Found %f skipped steps, while expected not more than %f percent",
                    skippedStepsThreshold, skippedStepsPercentage));
            return true;
        }
        double pendingStepsThreshold = 100.0 * result.getPendingSteps() / result.getSteps();
        if (pendingStepsThreshold > pendingStepsPercentage) {
            log(listener, String.format("Found %f pending steps, while expected not more than %f percent",
                    pendingStepsThreshold, pendingStepsPercentage));
            return true;
        }
        double undefinedStepsThreshold = 100.0 * result.getUndefinedSteps() / result.getSteps();
        if (undefinedStepsThreshold > undefinedStepsPercentage) {
            log(listener, String.format("Found %f undefined steps, while expected not more than %f percent",
                    undefinedStepsThreshold, undefinedStepsPercentage));
            return true;
        }
        double failedScenariosThreshold = 100.0 * result.getFailedScenarios() / result.getScenarios();
        if (failedScenariosThreshold > failedScenariosPercentage) {
            log(listener, String.format("Found %f failed scenarios, while expected not more than %f percent",
                    failedScenariosThreshold, failedScenariosPercentage));
            return true;
        }
        double failedFeaturesThreshold = 100.0 * result.getFailedFeatures() / result.getFeatures();
        if (failedFeaturesThreshold > failedFeaturesPercentage) {
            log(listener, String.format("Found %f failed features, while expected not more than %f percent",
                    failedFeaturesThreshold, failedFeaturesPercentage));
            return true;
        }

        return false;
    }

    private String evaluateMacro(Run<?, ?> build, FilePath workspace, TaskListener listener, String value) throws InterruptedException, IOException {
        try {
            return TokenMacro.expandAll(build, workspace, listener, value);
        } catch (MacroEvaluationException e) {
            log(listener, String.format("Could not evaluate macro '%s': %s", value, e.getMessage()));
        }
        return value;
    }

    private void addClassificationsToBuildReport(Run<?, ?> build, FilePath workspace, TaskListener listener, Configuration configuration, List<Classification> listToAdd) throws InterruptedException, IOException {
        for (Classification classification : listToAdd) {
            log(listener, String.format("Adding classification - %s -> %s", classification.key, classification.value));
            configuration.addClassifications(classification.key, evaluateMacro(build, workspace, listener, classification.value));
        }
    }

    private List<String> fetchPropertyFiles(File targetDirectory, TaskListener listener) {
        List<String> propertyFiles = new ArrayList<>();
        if (StringUtils.isNotEmpty(classificationsFilePattern)) {
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setIncludes(new String[]{classificationsFilePattern});
            scanner.setBasedir(targetDirectory);
            scanner.setCaseSensitive(false);
            scanner.scan();
            propertyFiles = getFullMetaDataPath(scanner.getIncludedFiles(), targetDirectory.toString());
            for (String propertyFile : propertyFiles) {
                log(listener, String.format("Found Properties File - %s ", propertyFile));
            }
        }
        return propertyFiles;
    }

    private List<String> getFullMetaDataPath(String[] files, String propertiesDirectory) {
        List<String> fullPathList = new ArrayList<>();
        for (String file : files) {
            fullPathList.add(propertiesDirectory + File.separator + file);
        }
        return fullPathList;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
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
    @Symbol("cucumber")
    public static class BuildStatusesDescriptorImpl extends CucumberReportDescriptor {
    }
}
