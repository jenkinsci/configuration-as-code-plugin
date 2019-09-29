# Configure GitHub

Basic configuration of the [GitHub plugin](https://plugins.jenkins.io/github)

## sample configuration

```yaml
unclassified:
  githubpluginconfig:
    configs:
      - name: "InHouse GitHub EE"
        apiUrl: "https://github.domain.local/api/v3"
        credentialsId: "[GitHubEEUser]"
        manageHooks: true
```

Please note that the _credentialsId_ takes the id of a set of credentials created as a "Secret Text Credential", i.e. a token from a GitHub user.
