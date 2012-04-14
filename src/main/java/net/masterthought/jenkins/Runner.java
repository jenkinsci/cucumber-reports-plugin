package net.masterthought.jenkins;


import java.io.File;
import java.io.IOException;

public class Runner {

    public static void main(String[] args) throws Exception {
        File rd = new File("/Users/kings/visa/jenkins-plugin-development/jenkins-java-plugin/cucumber-reports/work/jobs/ssss/builds/10/cucumber-html-reports");
       SingleResultParser singleResultParser = new SingleResultParser("/Users/kings/visa/jenkins-plugin-development/jenkins-java-plugin/cucumber-reports/work/jobs/ssss/builds/10/cucumber-html-reports/cucumber.json",rd,"10","ssss");
       singleResultParser.generateReports();

    }
}
