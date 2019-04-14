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
        - "View/Delete:authenticated"
        - "View/Read:authenticated"
        - "View/Configure:authenticated"
        - "View/Create:authenticated"
        - "Job/Read:authenticated"
        - "Job/Build:authenticated"
        - "Job/Configure:authenticated"
        - "Job/Create:authenticated"
        - "Job/Delete:authenticated"
        - "Job/Discover:authenticated"
        - "Job/Move:authenticated"
        - "Job/Workspace:authenticated"
        - "Job/Cancel:authenticated"
        - "Run/Delete:authenticated"
        - "Run/Replay:authenticated"
        - "Run/Update:authenticated"
        - "SCM/Tag:authenticated"
        - "Overall/Read:anonymous"
        - "Overall/Administer:authenticated"
```

Some permissions depends on actual plugin-usage.  
For Example: `Release/*:authenticated` is only available if you _use_ the Release plugin in one of your jobs.
