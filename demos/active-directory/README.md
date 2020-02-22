# Configure activeDirectory Security Realm

Basic configuration of the [Active Directory plugin](https://plugins.jenkins.io/active-directory)

For plugin version 2.12 and up:

## sample configuration

```yaml
jenkins:
  securityRealm:
    activeDirectory:
      domains:
        - name: "acme"
          servers: "ad1.acme.com:123,ad2.acme.com:456"
          site: "site"
          bindName: "admin"
          bindPassword: "${BIND_PASSWORD}"
          tlsConfiguration: JDK_TRUSTSTORE
      groupLookupStrategy: "RECURSIVE"
      removeIrrelevantGroups: true
      customDomain: true
      cache:
        size: 500
        ttl: 600
      startTls: true
      internalUsersDatabase:
        jenkinsInternalUser: "jenkins"
```
