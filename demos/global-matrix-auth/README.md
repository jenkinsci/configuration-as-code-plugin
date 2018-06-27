# matrix-auth-plugin

## sample-configuration (global matrix)

```yaml
jenkins:
  authorizationStrategy:
    globalMatrix:
      grantedPermissions:
        - name: "anonymous"
          permissions:
            - "Overall/Read"
        - name: "authenticated"
          permissions:
            - "Overall/Administer"
```

## sample-configuration (project based matrix)


```yaml
jenkins:
  authorizationStrategy:
    projectMatrix:
      grantedPermissions:
        - name: "anonymous"
          permissions:
            - "Overall/Read"
        - name: "authenticated"
          permissions:
            - "Overall/Administer"
```
