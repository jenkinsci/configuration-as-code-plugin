# matrix-auth-plugin

## sample-configuration (global matrix)

```yaml
jenkins:
  authorizationStrategy:
    globalMatrix:
      grantedPermissions:
        - "Overall/Read:anonymous"
        - "Overall/Administer:authenticated"
```

## sample-configuration (project based matrix)


```yaml
jenkins:
  authorizationStrategy:
    projectMatrix:
      grantedPermissions:
        - "Overall/Read:anonymous"
        - "Overall/Administer:authenticated"
```
