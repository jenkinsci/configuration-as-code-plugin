# CI automation

This folder contains the CI automation scripts for this plugin.

The pull request builder job that runs on this project on the Jenkins community infrastructure is found in [../Jenkinsfile](../Jenkinsfile). Job is here:   https://ci.jenkins.io/blue/organizations/jenkins/Plugins%2Fconfiguration-as-code-plugin/

The release job that performs the release of the plugin is hosted currently at [Praqma's](https://www.praqma.com/) Jenkins CI infrastructure and automates the release process.

Job is publicly available [here](http://code.praqma.net/ci/job/jenkins-configuration-as-code-plugin-release/)

Jenkinsfile is here [Jenkinsfile](/.ci/Jenkinsfile).

Job can only be executed by a few selected users.
