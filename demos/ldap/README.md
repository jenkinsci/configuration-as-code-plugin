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

`hudson.security.LDAPSecurityRealm` can be configured using its `@DataBoundConstructor` parameters without any dedicated
adapter code.
It is identified as `ldap` as it implements the `SecurityRealm` extension point, so we can define a "natural" symbol name
for it.

## sample configuration with search filters

```yaml
jenkins:
  securityRealm:
    ldap:
      configurations:
        - server: "ldap.acme.com"
          rootDN: "dc=acme,dc=fr"
          managerDN: "manager"
          managerPasswordSecret: ${LDAP_PASSWORD}
          userSearch: "(&(objectCategory=User)(sAMAccountName={0}))"
          groupSearchFilter: "(&(cn={0})(objectclass=group))"
          groupMembershipStrategy:
            fromGroupSearch:
              filter: "(&(objectClass=group)(|(cn=GROUP_1)(cn=GROUP_2)))"
```
