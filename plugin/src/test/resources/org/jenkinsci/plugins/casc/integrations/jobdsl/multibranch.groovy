package org.jenkinsci.plugins.casc.integrations.jobdsl

multibranchPipelineJob('configuration-as-code2') {
    branchSources {
        git {
            id = 'configuration-as-code'
            remote('https://github.com/jenkinsci/configuration-as-code-plugin.git')
        }
    }
}