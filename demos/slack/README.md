# Configure Slack

Basic configuration of the [Slack plugin](https://plugins.jenkins.io/slack)

## Sample configuration

```yaml
credentials:
  system:
    domainCredentials:
      - credentials:
          - string:
              scope: GLOBAL
              id: slack-token
              secret: "${SLACK_TOKEN}"
              description: Slack token

unclassified:
  slackNotifier:
    teamDomain: <your-slack-workspace-name> # i.e. your-company (just the workspace name not the full url)
    tokenCredentialId: slack-token
```
