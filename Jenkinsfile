pipeline {

    agent any

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
