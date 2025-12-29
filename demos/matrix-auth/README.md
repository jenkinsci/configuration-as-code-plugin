# matrix-auth-plugin

> **Note:** Starting from **matrix-auth 3.2**, the older  
> `grantedPermissions:` JCasC syntax is **deprecated**.  
> The examples below already use the new **`entries:`** format, which should be used going forward.


Configuration of the [Matrix Authorization Strategy plugin](https://plugins.jenkins.io/matrix-auth)

There are a couple of built-in authorizations to consider.

- **anonymous** - anyone who has not logged in.
- **authenticated** - anyone who has logged in.

## sample-configuration (global matrix)

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
              - "Job/Read"
        - group:
            name: "authenticated"
            permissions:
              - "Overall/Read"
              - "Job/Build"
              - "Job/Create"
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

Some permissions depends on actual plugin-usage.  
For Example: `Release/*:authenticated` is only available if you _use_ the Release plugin in one of your jobs.

## GitHub Authorization

https://plugins.jenkins.io/github-oauth/

You can configure authorization based on GitHub users, organizations, or teams.

- **username** - specific GitHub username.
- **organization** - every user that belongs to a specific GitHub organization.
- **organization*team** - specific GitHub team of a GitHub organization.
