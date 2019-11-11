# Configure Keycloak plugin

Basic configuration of the [Keycloak plugin](https://plugins.jenkins.io/keycloak)

## sample configuration

```yaml
jenkins:
  securityRealm: keycloak

unclassified:
  keycloakSecurityRealm:
    keycloakJson: |-
      {
        "realm": "my-realm",
        "auth-server-url": "https://my-keycloak-url/auth",
        "ssl-required": "all",
        "resource": "jenkins",
        "public-client": true,
        "confidential-port": 0
      }
```
