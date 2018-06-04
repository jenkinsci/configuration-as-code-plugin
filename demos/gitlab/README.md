# configure gitlab plugin

## sample configuration

```yaml
jenkins: [...]
credentials:
  system:
    ? # "global"
    : - GitLabApiTokenImpl:
         scope: SYSTEM
         id: gitlab_token
         apiToken: "qwertyuiopasdfghjklzxcvbnm"
         description: "Gitlab Token"

unclassified:
    gitlabconnectionconfig:
      connections:
        - gitlab-socrate:
          apiTokenId: "gitlab_token"
          name: "my_gitlab_server"
          url: "https://gitlab.com"
          readTimeout: "10"
          connectionTimeout: "10"
          ignoreCertificateErrors: true
          clientBuilderId: "autodetect"
```


