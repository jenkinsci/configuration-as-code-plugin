package io.jenkins.plugins.casc.step

pipeline {
    agent { label 'master' }
    stages {
        stage('Load Configuration') {
            steps {
                configurationAsCode targets: [
                    [env.CASC_FOLDER, "systemMessage.yml"].join(File.separator),
                    [env.CASC_FOLDER, "scmCheckoutRetryCount.yml"].join(File.separator)
                ]
            }
        }
    }
}
