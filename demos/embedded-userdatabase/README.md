# configure Jenkins’ own user database

_Note: You can disable exporting of users by setting the following system property:_

 `-Dio.jenkins.plugins.casc.core.HudsonPrivateSecurityRealmConfigurator.exportUsers=false`

## sample configuration

```yaml
jenkins:
  securityRealm:
    local:
      allowsSignup: false
      users:
        - id: "admin"
          password: "somethingsecret"
  authorizationStrategy: loggedInUsersCanDoAnything
```

### Additional attributes

```yaml
jenkins:
  securityRealm:
    local:
      allowsSignup: false
      users:
        - id: "hashedadmin"
          # password is 'password'
          password: "#jbcrypt:$2a$10$LP4bMhwyCPnsDm.XRcTZSuBqWYKGAiDAsQXrSrJGYcEd9padaPgsC"
        - id: "admin"
          name: "Admin"
          description: "Superwoman"
          password: "somethingsecret"
          properties:
            - mailer:
                emailAddress: "admin3@example.com"
            - preferredProvider:
                providerId: "default"
            - slack:
                userId: "ABCDEFGH"
            #- timezone: # pending https://github.com/jenkinsci/jenkins/pull/4557
            #    timeZoneName: "Europe/London"
            #- sshPublicKey: # pending https://github.com/jenkinsci/ssh-cli-auth-module/pull/16
            #    authorizedKeys: |
            #      ssh-rsa some-key
  authorizationStrategy: loggedInUsersCanDoAnything
```
