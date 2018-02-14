# matrix-auth-plugin

## Limitations

Project based matrix authentication is on it's way.

## sample-configuration

```yaml
jenkins:
  authorizationStrategy:
    globalMatrix:
      grantedPermissions:
       - group:
           name: "anonymous"
           permissions:
            - "Overall/Read"
       - group
           name: "authenticated"
           permissions:
            - "Overall/Administer"
```