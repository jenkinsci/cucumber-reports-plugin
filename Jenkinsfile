pipeline {

    agent any

    tools {
        maven 'maven 3'
        jdk 'java 8'
    }

    stages {
        stage ('Build') {

            git url: 'https://github.com/cyrille-leclerc/multi-module-maven-project'

            withMaven(
                // Maven installation declared in the Jenkins "Global Tool Configuration"
                maven: 'M3',
                // Maven settings.xml file defined with the Jenkins Config File Provider Plugin
                // Maven settings and global settings can also be defined in Jenkins Global Tools Configuration
                mavenSettingsConfig: 'my-maven-settings',
                mavenLocalRepo: '.repository') {

              // Run the maven build
              sh "mvn clean install"

            } // withMaven will discover the generated Maven artifacts, JUnit Surefire & FailSafe reports and FindBugs reports
          }
    }
}
