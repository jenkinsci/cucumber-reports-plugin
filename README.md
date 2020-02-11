[![Build Travis](https://img.shields.io/travis/jenkinsci/cucumber-reports-plugin/master.svg?label=Travis%20bulid)](https://travis-ci.org/jenkinsci/cucumber-reports-plugin)
[![Build Shippable](https://img.shields.io/shippable/540e74493479c5ea8f9e5f55/master.svg?label=Shippable%20bulid)](https://app.shippable.com/github/jenkinsci/cucumber-reports-plugin/)

[![Popularity](https://img.shields.io/jenkins/plugin/i/cucumber-reports.svg)](https://plugins.jenkins.io/cucumber-reports)
[![Live Demo](https://img.shields.io/badge/Live%20Demo-Online-blue.svg)](http://damianszczepanik.github.io/cucumber-html-reports/overview-features.html)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/8a9f4e032a47461fb984cd39c599584d)](https://www.codacy.com/app/damianszczepanik/cucumber-reports-plugin?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=jenkinsci/cucumber-reports-plugin&amp;utm_campaign=Badge_Grade)

# Publish pretty [cucumber](https://cucumber.io/) reports on [Jenkins](http://jenkins-ci.org/)

This is a Java Jenkins plugin which publishes [pretty html reports](https://github.com/damianszczepanik/cucumber-reporting) showing the results of cucumber runs. To use with regular cucumber just make sure to run cucumber like this: cucumber `--plugin json -o cucumber.json`

## Background

Cucumber is a test automation tool following the principles of [Behavioural Driven Design](https://en.wikipedia.org/wiki/Behavior-driven_development) and living documentation. Specifications are written in a concise [human readable form](https://cucumber.io/docs/reference) and executed in continuous integration. 

This plugin allows Jenkins to publish the results as pretty html reports hosted by the Jenkins build server. In order for this plugin to work you must be using the JUnit runner and generating a json report. The plugin converts the json report into an overview html linking to separate feature file htmls with stats and results. 

## Install

1.  [Get](https://jenkins-ci.org/) Jenkins.
2.  Install the [Cucumber Reports](https://wiki.jenkins-ci.org/display/JENKINS/Cucumber+Reports+Plugin) plugin.
3.  Restart Jenkins.

Read this if you need further  [detailed install and configuration](https://github.com/jenkinsci/cucumber-reports-plugin/wiki/Detailed-Configuration) instructions 

## Use
You must use a **Freestyle project type** in jenkins.

With the cucumber-reports plugin installed in Jenkins, you simply check the "Publish cucumber results as a report" box in the
publish section of the build config:

![](.README/publish-box.png)

If you need more control over the plugin you can click the Advanced button for more options:

![](.README/advanced-publish-box.png)

1.  Report title can be used to publish multiple reports from the same job - reports with different titles are stored separately; or leave blank for a single report with no title
2.  Leave empty for the plugin to automagically find your json files or enter the path to the json reports relative to the workspace if for some reason the automagic doesn't work for you
3.  Leave empty unless your jenkins is installed on a different url to the default hostname:port - see the wiki for further info on this option
4.  Tick if you want Skipped steps to cause the build to fail - see further down for more info on this
5.  Tick if you want Not Implemented/Pending steps to cause the build to fail - see further down for more info on this
6.  Tick if you want failed test not to fail the entire build but make it unstable

## Advanced Configuration Options

There are 4 advanced configuration options that can affect the outcome of the build status. Click on the Advanced tab in the configuration screen:

![Advanced Configuration](.README/advanced_options.png)

The first setting is Skipped steps fail the build - so if you tick this any steps that are skipped during executions will be marked as failed and will cause the build to fail:

If you check both skipped and not implemented fails the build then your report will look something like this:

Make sure you have configured cucumber to run with the JUnit runner and to generate a json report: (note - you can add other formatters in if you like e.g. pretty - but only the json formatter is required for the reports to work)
```java
  import cucumber.junit.Cucumber;
  import org.junit.runner.RunWith;
  
  @RunWith(Cucumber.class)
  @Cucumber.Options(format = {"json:target/cucumber.json"})
  public class MyTest {
  
  }
```

## Automated configuration

### Pipeline usage

Typical step for report generation:
```groovy
node {
    stage('Generate HTML report') {
        cucumber buildStatus: 'UNSTABLE',
                reportTitle: 'My report',
                fileIncludePattern: '**/*.json',
                trendsLimit: 10,
                classifications: [
                    [
                        'key': 'Browser',
                        'value': 'Firefox'
                    ]
                ]
    }
}
```
or post action when the build completes with some fancy features for the Gerrit integraion:
```groovy
post {
    always {
        cucumber buildStatus: 'UNSTABLE',
                failedFeaturesNumber: 1,
                failedScenariosNumber: 1,
                skippedStepsNumber: 1,
                failedStepsNumber: 1,
                classifications: [
                        [key: 'Commit', value: '<a href="${GERRIT_CHANGE_URL}">${GERRIT_PATCHSET_REVISION}</a>'],
                        [key: 'Submitter', value: '${GERRIT_PATCHSET_UPLOADER_NAME}']
                ],
                reportTitle: 'My report',
                fileIncludePattern: '**/*cucumber-report.json',
                sortingMethod: 'ALPHABETICAL',
                trendsLimit: 100
    }
}
 ```

### Raw DSL - This should be utilized after build steps (note that the title is not specified in this example)

```groovy
configure { project ->
  project / 'publishers' << 'net.masterthought.jenkins.CucumberReportPublisher' {
    fileIncludePattern '**/*.json'
    fileExcludePattern ''
    jsonReportDirectory ''
    failedStepsNumber '0'
    skippedStepsNumber '0'
    pendingStepsNumber '0'
    undefinedStepsNumber '0'
    failedScenariosNumber '0'
    failedFeaturesNumber '0'
    buildStatus 'FAILURE'  //other option is 'UNSTABLE' - if you'd like it left unchanged, don't provide a value
    trendsLimit '0'
    sortingMethod 'ALPHABETICAL'
  }
}
```

When a build runs that publishes cucumber results it will put a link in the sidepanel to the [cucumber reports](https://github.com/damianszczepanik/cucumber-reporting). There is a feature overview page:

![feature overview page](https://github.com/damianszczepanik/cucumber-reporting/raw/master/.README/feature-overview.png)

And there are also feature specific results pages:

![feature specific page passing](https://github.com/damianszczepanik/cucumber-reporting/raw/master/.README/feature-passed.png)

And useful information for failures:

![feature specific page failing](https://github.com/damianszczepanik/cucumber-reporting/raw/master/.README/feature-failed.png)

If you have tags in your cucumber features you can see a tag overview:

![Tag overview](https://github.com/damianszczepanik/cucumber-reporting/raw/master/.README/tag-overview.png)

And you can drill down into tag specific reports:

![Tag report](https://github.com/damianszczepanik/cucumber-reporting/raw/master/.README/tag-report.png)

## Develop

Interested in contributing to the Jenkins cucumber-reports plugin?  Great!  Start [here](https://github.com/jenkinsci/cucumber-reports-plugin).
