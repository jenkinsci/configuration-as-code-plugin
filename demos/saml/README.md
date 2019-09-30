# Configure saml2.0 plugin

Basic configuration of the [SAML plugin](https://plugins.jenkins.io/saml)

## sample configuration

```yaml
jenkins:
  securityRealm:
    saml:
      binding: "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect"
      displayNameAttributeName: "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name"
      emailAttributeName: "Email"
      groupsAttributeName: "http://schemas.xmlsoap.org/claims/Group"
      idpMetadataConfiguration:
        xml: "<todo>for testing purposes</todo>" ## In order to test the demo within the integrations.
        period: 0
        url: "https://abc.com"
      maximumAuthenticationLifetime: 86400
      usernameAttributeName: "NameID"
      usernameCaseConversion: "none"
```