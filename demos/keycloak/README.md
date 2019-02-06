# configure Keycloak plugin

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

