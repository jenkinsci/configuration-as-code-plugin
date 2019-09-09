# configure Jenkins’ own user database

## sample configuration

```yaml
jenkins:
  securityRealm:
    local:
      allowsSignup: false
      users:
        - id: "admin"
          password: "somethingsecret"
  authorizationStrategy: loggedInUsersCanDoAnything
```
