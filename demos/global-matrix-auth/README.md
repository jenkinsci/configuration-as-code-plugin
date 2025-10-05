# matrix-auth-plugin

Requires `matrix-auth` >= 3.2
> Starting from version 3.2 of the `matrix-auth` plugin, the JCasC syntax for configuring permissions has changed. The previous `permissions:` format is deprecated and replaced with a structured `entries:` format. While older configurations may still work with `deprecated: warn`, it is recommended to migrate to the new format.

There are a couple of built-in authorizations to consider.

- **anonymous** - anyone who has not logged in. 
- **authenticated** - anyone who has logged in. 

## sample-configuration (global matrix)

Updated Configuration:
```yaml
jenkins:
  authorizationStrategy:
    globalMatrix:
      entries:
        - user:
            name: "admin"
            permissions:
              - "Overall/Administer"
        - user:
            name: "anonymous"
            permissions:
              - "Overall/Read"
        - group:
            name: "authenticated"
            permissions:
              - "Overall/Administer"
```
Permissions must be defined **per line**, meaning each line must grant permission to only a single role, and only a single user or group of users.



## Deprecated Configuration (Pre-3.2)
```yaml
jenkins:
  authorizationStrategy:
    globalMatrix:
      permissions:
        - "USER:Overall/Read:anonymous"
        - "GROUP:Overall/Administer:authenticated"
        - "USER:Overall/Administer:admin"
```



## sample-configuration (project based matrix)

```yaml
jenkins:
  authorizationStrategy:
    projectMatrix:
      entries:
        - group:
            name: "authenticated"
            permissions:
              - "View/Delete"
              - "View/Read"
              - "View/Configure"
              - "View/Create"
              - "Job/Read"
              - "Job/Build"
              - "Job/Configure"
              - "Job/Create"
              - "Job/Delete"
              - "Job/Discover"
              - "Job/Move"
              - "Job/Workspace"
              - "Job/Cancel"
              - "Run/Delete"
              - "Run/Replay"
              - "Run/Update"
              - "SCM/Tag"
              - "Overall/Administer"
        - user:
            name: "anonymous"
            permissions:
              - "Overall/Read"
```

## Deprecated Configuration for Project Matrix (Pre-3.2)
```yaml
jenkins:
  authorizationStrategy:
    projectMatrix:
      permissions:
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

## GitHub Authorization

https://plugins.jenkins.io/github-oauth/

You can configure authorization based on GitHub users, organizations, or teams.

- **username** - specific GitHub username.
- **organization** - every user that belongs to a specific GitHub organization.
- **organization*team** - specific GitHub team of a GitHub organization.
