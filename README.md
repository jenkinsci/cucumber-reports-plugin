[![Build Travis](https://img.shields.io/travis/jenkinsci/cucumber-reports-plugin/master.svg)](https://travis-ci.org/jenkinsci/cucumber-reports-plugin)
[![Build Jenkins](https://jenkins.ci.cloudbees.com/job/plugins/job/cucumber-reports-plugin/badge/icon)](https://jenkins.ci.cloudbees.com/job/plugins/job/cucumber-reports-plugin/)

[![Maven Dependencies](https://www.versioneye.com/user/projects/5663e781f376cc003d0009df/badge.svg)](https://www.versioneye.com/user/projects/5663e781f376cc003d0009df?child=summary)

# Publish pretty [cucumber](https://cucumber.io/) reports on [Jenkins](http://jenkins-ci.org/)

This is a Java Jenkins plugin which publishes [pretty html reports](https://github.com/damianszczepanik/cucumber-reporting) showing the results of cucumber runs. To use with regular cucumber just make sure to run cucumber like this: cucumber `--format json -o cucumber.json`

## Background

Cucumber is a test automation tool following the principles of [Behavioural Driven Design](https://en.wikipedia.org/wiki/Behavior-driven_development) and living documentation. Specifications are written in a concise [human readable form](https://cucumber.io/docs/reference) and executed in continuous integration. 

This plugin allows Jenkins to publish the results as pretty html reports hosted by the Jenkins build server. In order for this plugin to work you must be using the JUnit runner and generating a json report. The plugin converts the json report into an overview html linking to separate feature file htmls with stats and results. 

## Install

1. [Get](https://jenkins-ci.org/) Jenkins.

2. Install the [Cucumber Reports](https://wiki.jenkins-ci.org/display/JENKINS/Cucumber+Reports+Plugin) plugin.

3. Restart Jenkins.

Read this if you need further  [detailed install and configuration](https://github.com/jenkinsci/cucumber-reports-plugin/wiki/Detailed-Configuration) instructions 

## Use
You must use a **Freestyle project type** in jenkins.

With the cucumber-reports plugin installed in Jenkins, you simply check the "Publish cucumber results as a report" box in the
publish section of the build config:

(https://github.com/jenkinsci/cucumber-reports-plugin/raw/master/.README/publish-box.png)

If you need more control over the plugin you can click the Advanced button for more options:

(https://github.com/jenkinsci/cucumber-reports-plugin/raw/master/.README/advanced-publish-box.png)

1. Leave empty for the plugin to automagically find your json files or enter the path to the json reports relative to the workspace if for some reason the automagic doesn't work for you
2. Leave empty unless your jenkins is installed on a different url to the default hostname:port - see the wiki for further info on this option
3. Tick if you want Skipped steps to cause the build to fail - see further down for more info on this
4. Tick if you want Not Implemented/Pending steps to cause the build to fail - see further down for more info on this
5. Tick if you want failed test not to fail the entire build but make it unstable

When a build runs that publishes cucumber results it will put a link in the sidepanel to the [cucumber reports](https://github.com/damianszczepanik/cucumber-reporting). There is a feature overview page:

![feature overview page]
(https://github.com/damianszczepanik/cucumber-reporting/raw/master/.README/feature-overview.png)

And there are also feature specific results pages:

![feature specific page passing]
(https://github.com/damianszczepanik/cucumber-reporting/raw/master/.README/feature-passed.png)

And useful information for failures:

![feature specific page failing]
(https://github.com/damianszczepanik/cucumber-reporting/raw/master/.README/feature-failed.png)

If you have tags in your cucumber features you can see a tag overview:

![Tag overview]
(https://github.com/damianszczepanik/cucumber-reporting/raw/master/.README/tag-overview.png)

And you can drill down into tag specific reports:

![Tag report]
(https://github.com/damianszczepanik/cucumber-reporting/raw/master/.README/tag-report.png)

## Advanced Configuration Options

There are 4 advanced configuration options that can affect the outcome of the build status. Click on the Advanced tab in the configuration screen:

![Advanced Configuration]
(https://github.com/jenkinsci/cucumber-reports-plugin/raw/master/.README/advanced_options.png)

The first setting is Skipped steps fail the build - so if you tick this any steps that are skipped during executions will be marked as failed and will cause the build to fail:

If you check both skipped and not implemented fails the build then your report will look something like this:


Make sure you have configured cucumber to run with the JUnit runner and to generate a json report: (note - you can add other formatters in if you like e.g. pretty - but only the json formatter is required for the reports to work)

    package net.masterthought.example;

    import cucumber.junit.Cucumber;
    import org.junit.runner.RunWith;

    @RunWith(Cucumber.class)
    @Cucumber.Options(format = {"json:target/cucumber.json"})
    public class ATMTest {
    }

## Develop

Interested in contributing to the Jenkins cucumber-reports plugin?  Great!  Start [here]
(https://github.com/jenkinsci/cucumber-reports-plugin).
