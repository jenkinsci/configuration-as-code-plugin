# Configure Slack

Basic configuration of the [Slack Plugin](https://wiki.jenkins.io/display/JENKINS/Slack+Plugin)

## Sample configuration

```yaml
unclassified:
  slackNotifier:
    baseUrl: https://workspace.slack.com/services/hooks/jenkins-ci/
    teamDomain: workspace
    botUser: true
    tokenCredentialId: "SecretTextCredentialsId
```
