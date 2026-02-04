# matrix-auth-plugin

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

### Important note about Job/Create permission

The `Job/Create` permission allows a user to start the job creation process.
However, it does not allow saving or modifying the job configuration.

If `Job/Create` is granted without `Job/Configure`:
- The user can click **New Item**
- An authorization error may appear when saving
- The job may still appear in the dashboard
- This is expected Jenkins behavior

To allow users to fully create and configure jobs, both
`Job/Create` and `Job/Configure` should be granted.

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
