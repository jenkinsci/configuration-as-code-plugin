# Configure Gitea plugin

Basic configuration of the [Gitea plugin](https://plugins.jenkins.io/gitea/)

## sample configuration

```yaml
unclassified:
  giteaServers:
    servers:
    - credentialsId: <my-credential-id>
      displayName: scm
      manageHooks: true
      serverUrl: https://my-scm-url
```
