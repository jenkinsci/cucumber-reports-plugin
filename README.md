# Publish pretty [cucumber-jvm](https://github.com/cucumber/cucumber-jvm) reports on [Jenkins](http://jenkins-ci.org/)

This is a Java Jenkins plugin which publishes pretty html reports showing the results of cucumber-jvm runs.


## Background

Cucumber-JVM is a test automation tool following the principles of Behavioural Driven Design and living documentation. Specifications are written in a concise human readable form and executed in continuous integration. 

This plugin allows Jenkins to publish the results as pretty html reports hosted by the Jenkins build server. In order for this plugin to work you must be using the JUnit runner and generating a json report. The plugin converts the json report into an overview html linking to separate feature file htmls with stats and results. 

## Install

1. [Get](https://jenkins-ci.org/) Jenkins.

2. Install the [cucumber-jvm-reports-java]
(https://github.com/masterthought/jenkins-cucumber-jvm-reports-plugin-java/downloads) plugin.

3. Restart Jenkins.

## Use
You must use a Freestyle project type in jenkins.

With the cucumber-jvm-reports plugin installed in Jenkins, you simply check the "Publish cucumber results as a report" box in the
publish section of the build config and enter the path to the json reports relative to the workspace:

![check the publish cucumber-jvm-reports box]
(https://github.com/masterthought/jenkins-cucumber-jvm-reports-plugin/raw/master/.README/publish-box.png)

When a build runs that publishes cucumber-jvm results it will put a link in the sidepanel to the cucumber reports. There is a feature overview page:

![feature overview page]
(https://github.com/masterthought/jenkins-cucumber-jvm-reports-plugin/raw/master/.README/feature-overview.png)

And there are also feature specific results pages:

![feature specific page passing]
(https://github.com/masterthought/jenkins-cucumber-jvm-reports-plugin/raw/master/.README/feature-passed.png)

And useful information for failures:

![feature specific page passing]
(https://github.com/masterthought/jenkins-cucumber-jvm-reports-plugin/raw/master/.README/feature-failed.png)

If you have tags in your cucumber features you can see a tag overview:

![Tag overview]
(https://github.com/masterthought/jenkins-cucumber-jvm-reports-plugin/raw/master/.README/tag-overview.png)

And you can drill down into tag specific reports:

![Tag report]
(https://github.com/masterthought/jenkins-cucumber-jvm-reports-plugin/raw/master/.README/tag-report.png)

Make sure you have configured cucumber-jvm to run with the JUnit runner and to generate a json report:

    package net.masterthought.example;

    import cucumber.junit.Cucumber;
    import org.junit.runner.RunWith;

    @RunWith(Cucumber.class)
    @Cucumber.Options(format = {"pretty", "html:target/cucumber", "json:target/cucumber.json"})
    public class ATMTest {
    }

## Develop

Interested in contributing to the Jenkins cucumber-jvm-reports plugin?  Great!  Start [here]
(https://github.com/masterthought/jenkins-cucumber-jvm-reports-plugin-java).
