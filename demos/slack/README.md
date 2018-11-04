# Configure Slack

Basic configuration of the [Slack Plugin](https://wiki.jenkins.io/display/JENKINS/Slack+Plugin)

## Sample configuration

```yaml
credentials:
  system:
    domainCredentials:
      - credentials:
          - string:
              scope: GLOBAL
              id: slack-token
              secret: '${SLACK_TOKEN}'
              description: Slack token


unclassified:
  slackNotifier:
    teamDomain: <your-domain>
    tokenCredentialId: <secret-text-token>
```
