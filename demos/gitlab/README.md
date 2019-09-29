# Configure gitlab plugin

Basic configuration of the [Gitlab plugin](https://plugins.jenkins.io/gitlab-plugin)

## sample configuration

```yaml
credentials:
  system:
    domainCredentials:
      - credentials:
          - gitLabApiTokenImpl:
              scope: SYSTEM
              id: gitlab_token
              apiToken: "${BIND_TOKEN}"
              description: "Gitlab Token"
unclassified:
  gitlabconnectionconfig:
    connections:
      - apiTokenId: gitlab_token
        clientBuilderId: "autodetect"
        connectionTimeout: 20
        ignoreCertificateErrors: true
        name: "my_gitlab_server"
        readTimeout: 10
        url: "https://gitlab.com/"
```
