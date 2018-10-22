# google-login-plugin

Example configuration of the [Google Login plugin](https://plugins.jenkins.io/google-login),
providing oauth authorization to Jenkins.


## sample-configuration

```yaml
jenkins:
  securityRealm:
    googleOAuth2:
      clientId: "___.apps.googleusercontent.com"
      clientSecret: "${GOOGLE_OAUTH_SECRET}"
```
