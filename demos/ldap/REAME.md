# configure ldap plugin

## sample configuration

```yaml
jenkins:

  securityRealm:
    ldap:
      configurations:
        - server: ldap.acme.com
          rootDN: dc=acme,dc=fr
          managerPasswordSecret: ${LDAP_PASSWORD}
      cache:
        size: 100
        ttl: 10
      userIdStrategy: CaseSensitive
      groupIdStrategy: CaseSensitive
```

## implementation note

`hudson.security.LDAPSecurityRealm` can be configure using it's @DataBoundConstructor parameters without any dedicated
adapter code.
It is identified as `ldap` as it implements `SecurityRealm` extension point, so we can define a "natural" Symbol name 
for it.  
