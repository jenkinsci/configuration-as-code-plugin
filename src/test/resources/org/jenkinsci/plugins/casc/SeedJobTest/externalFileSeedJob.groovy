multibranchPipelineJob('configuration-as-code') {
    branchSources {
        git {
            remote('https://github.com/jenkinsci/configuration-as-code-plugin.git')
        }
    }
}