# google-login-plugin

Example configuration of the [Google Login plugin](https://plugins.jenkins.io/google-login),
providing oauth authorization to Jenkins.

For more info on what values to use, follow the instructions in the [README](https://github.com/jenkinsci/google-login-plugin) of the plugin.

## sample-configuration

```yaml
jenkins:
  securityRealm:
    googleOAuth2:
      clientId: "___.apps.googleusercontent.com"
      clientSecret: "${GOOGLE_OAUTH_SECRET}"
```
