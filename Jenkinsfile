pipeline {

    agent any

    tools {
        maven 'maven 3'
        jdk 'java 8'
    }

    stages {
        stage('Build') {
            steps {
                sh 'mvn clean verify'
            }
        }
        steps("Test") {
            steps {
            step([$class: 'CoberturaPublisher', autoUpdateHealth: false, autoUpdateStability: false, coberturaReportFile: 'fdsfsdfds', failUnhealthy: false, failUnstable: false, maxNumberOfBuilds: 0, onlyStable: false, sourceEncoding: 'ASCII', zoomCoverageChart: false])
            }
        }
    }
}
