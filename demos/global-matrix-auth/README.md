# matrix-auth-plugin

Requires `matrix-auth` >= 2.4

There are a couple of built-in authorizations to consider.

- **anonymous** - anyone who has not logged in. 
- **authenticated** - anyone who has logged in. 

## sample-configuration (global matrix)

```yaml
jenkins:
  authorizationStrategy:
    globalMatrix:
      permissions:
        - "Overall/Read:anonymous"
        - "Overall/Administer:authenticated"
```
Permissions must be defined **per line**, meaning each line must grant permission to only a single role, and only a single user or group of users.

## sample-configuration (project based matrix)

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
