# Configure Crowd2 plugin

Basic configuration of the [Crowd2 plugin](https://plugins.jenkins.io/crowd2)

## sample configuration

```yaml
jenkins:
  securityRealm:
    crowd:
      applicationName: "jenkins"
      group: "jenkins-users"
      password: "${CROWD_PASSWORD}"
      url: "http://crowd.company.io"
```
