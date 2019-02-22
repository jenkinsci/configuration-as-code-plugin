# Configure activeDirectory Security Realm

Basic configuration of the [Active Directory plugin](https://wiki.jenkins.io/display/JENKINS/Active+Directory+Plugin)

## sample configuration

For Active Directory Plugin version 2.12 and up:
```yaml
jenkins:
  SecurityRealm:
    activeDirectory:
      groupLookupStrategy: AUTO
      startTls: true
      domains:
        - name: "domain.local"
          servers: "server.acme.com:3128"
          tlsConfiguration: TRUST_ALL_CERTIFICATES
```
