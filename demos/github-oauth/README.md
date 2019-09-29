# Configure github-oauth-plugin

Basic configuration of the [Active Directory plugin](https://plugins.jenkins.io/github-oauth)

## sample-configuration

```yaml
jenkins:
  securityRealm:
    github:
      githubWebUri: "https://github.com"
      githubApiUri: "https://api.github.com"
      clientID: "someId"
      clientSecret: "${GITHUB_SECRET}"
      oauthScopes: "read:org,user:email"
```
