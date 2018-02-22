# matrix-auth-plugin

## sample-configuration (global matrix)

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

## sample-configuration (project based matrix)


```yaml
jenkins:
  authorizationStrategy:
    projectMatrix:
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
