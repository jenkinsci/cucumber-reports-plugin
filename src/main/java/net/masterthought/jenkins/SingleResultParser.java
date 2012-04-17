package net.masterthought.jenkins;

import com.google.gson.Gson;
import net.masterthought.jenkins.json.Element;
import net.masterthought.jenkins.json.Feature;
import net.masterthought.jenkins.json.Step;
import net.masterthought.jenkins.json.Util;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class SingleResultParser {

    private List<Feature> featureList;
    private File reportDirectory;
    private String buildNumber;
    private String buildProject;
    private List<Util.Status> totalSteps;
    private String pluginUrlPath;

    public SingleResultParser(String jsonResultFile, File reportDirectory, String pluginUrlPath, String buildNumber, String buildProject) throws IOException {
        this.featureList = parseJson(jsonResultFile);
        this.totalSteps = getAllStepStatuses();
        this.reportDirectory = reportDirectory;
        this.buildNumber = buildNumber;
        this.buildProject = buildProject;
        this.pluginUrlPath = getPluginUrlPath(pluginUrlPath);
    }

    public void generateReports() throws Exception {
        generateFeatureReports();
        generateFeatureOverview();
    }

    public void generateFeatureOverview() throws Exception {
        VelocityEngine ve = new VelocityEngine();
        ve.init(getProperties());
        Template featureOverview = ve.getTemplate("templates/featureOverview.vm");
        VelocityContext context = new VelocityContext();
        context.put("build_project", buildProject);
        context.put("build_number", buildNumber);
        context.put("features", featureList);
        context.put("total_features", getTotalFeatures());
        context.put("total_scenarios", getTotalScenarios());
        context.put("total_steps", getTotalSteps());
        context.put("total_passes", getTotalPasses());
        context.put("total_fails", getTotalFails());
        context.put("total_skipped", getTotalSkipped());
        context.put("chart_data", XmlChartBuilder.donutChart(getTotalPasses(), getTotalFails(), getTotalSkipped()));
        context.put("time_stamp", timeStamp());
        context.put("jenkins_base", pluginUrlPath);
        generateReport("feature-overview.html", featureOverview, context);
    }

    public void generateFeatureReports() throws Exception {
        for (Feature feature : featureList) {
            VelocityEngine ve = new VelocityEngine();
            ve.init(getProperties());
            Template featureResult = ve.getTemplate("templates/featureReport.vm");
            VelocityContext context = new VelocityContext();
            context.put("feature", feature);
            context.put("report_status_colour", getReportStatusColour(feature));
            context.put("build_project", buildProject);
            context.put("build_number", buildNumber);
            context.put("scenarios", feature.getElements());
            context.put("time_stamp", timeStamp());
            context.put("jenkins_base", pluginUrlPath);
            generateReport(feature.getFileName(), featureResult, context);
        }
    }
    
    
    private String getPluginUrlPath(String path){
      return path.isEmpty() ? "/" : path;  
    }
    
    private int getTotalSteps() {
        return totalSteps.size();
    }

    private int getTotalPasses() {
        return Util.findStatusCount(totalSteps, Util.Status.PASSED);
    }

    private int getTotalFails() {
        return Util.findStatusCount(totalSteps, Util.Status.FAILED);
    }

    private int getTotalSkipped() {
        return Util.findStatusCount(totalSteps, Util.Status.SKIPPED);
    }

    private List<Util.Status> getAllStepStatuses() {
        List<Util.Status> steps = new ArrayList<Util.Status>();
        for (Feature feature : featureList) {
            for (Element scenario : feature.getElements()) {
                for (Step step : scenario.getSteps()) {
                    steps.add(step.getStatus());
                }
            }
        }
        return steps;
    }

    private int getTotalFeatures() {
        return featureList.size();
    }

    private int getTotalScenarios() {
        int scenarios = 0;
        for (Feature feature : featureList) {
            scenarios = scenarios + feature.getNumberOfScenarios();
        }
        return scenarios;
    }

    private void generateReport(String fileName, Template featureResult, VelocityContext context) throws Exception {
        Writer writer = new FileWriter(new File(reportDirectory, fileName));
        featureResult.merge(context, writer);
        writer.flush();
        writer.close();
    }

    private Properties getProperties() {
        Properties props = new Properties();
        props.setProperty("resource.loader", "class");
        props.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        return props;
    }

    private String getReportStatusColour(Feature feature) {
        return feature.getStatus() == Util.Status.PASSED ? "#C5D88A" : "#D88A8A";
    }

    private List<Feature> parseJson(String jsonResultFile) throws FileNotFoundException {
        FileReader reader = new FileReader(jsonResultFile);
        return Arrays.asList(new Gson().fromJson(reader, Feature[].class));
    }

    private String timeStamp() {
        return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
    }

}

