# Configure Matrix Authorization Strategy

Basic configuration of the [Matrix Authorization Strategy plugin](https://plugins.jenkins.io/matrix-auth)

## sample configuration

```yaml
jenkins:
  securityRealm:
    local:
      allowsSignup: false
      users:
        - id: test
          password: test

  authorizationStrategy:
    globalMatrix:
      permissions:
        - "Overall/Read:anonymous"
        - "Overall/Administer:authenticated"
```
