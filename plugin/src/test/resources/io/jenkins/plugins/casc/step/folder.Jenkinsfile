package io.jenkins.plugins.casc.step

pipeline {
    agent { label 'master' }
    stages {
        stage('Load Configuration') {
            steps {
                configurationAsCode targets: ["${env.CASC_FOLDER}"]
            }
        }
    }
}
