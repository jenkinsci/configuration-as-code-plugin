# Configure activeDirectory Security Realm

Basic configuration of the [Active Directory plugin](https://wiki.jenkins.io/display/JENKINS/Active+Directory+Plugin)

## sample configuration

```yaml
jenkins:
  SecurityRealm:
    activeDirectory:
      groupLookupStrategy: AUTO
      startTls: true
      tlsConfiguration: TRUST_ALL_CERTIFICATES
      domains:
        - name: "domain.local"
```