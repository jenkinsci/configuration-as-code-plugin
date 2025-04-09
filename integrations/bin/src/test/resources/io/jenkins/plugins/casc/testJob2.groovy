package io.jenkins.plugins.casc

job('testJob2') {
    steps {
        maven('-e clean test')
    }
}
