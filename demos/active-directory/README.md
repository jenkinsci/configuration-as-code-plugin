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
      groupLookupStrategy: "RECURSIVE"
      removeIrrelevantGroups: true
      customDomain: true
      cache:
        size: 4096
        ttl: 30
      startTls: true
      tlsConfiguration: JDK_TRUSTSTORE
      internalUsersDatabase:
        jenkinsInternalUser: "jenkins"
```
