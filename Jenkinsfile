buildPlugin(jenkinsVersions: [null, "2.107.1"], timeout: 180)

// Validate Demos
node("linux") {
    stage('Checkout') {
        checkout scm
    }

    stage('Validate demos') {
        sh 'bash ./demos/run.bash || echo "=== They are test failures"'
    }
}
