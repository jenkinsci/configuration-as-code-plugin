# configure oic-auth plugin

## automatic configuration
This configuration will read the well-known URL result to automatically configure the plugin.
```yaml
jenkins:
  securityRealm:
    oic:
      clientId: "clientid-from-your-openid-provider"
      clientSecret: "the-secret-of-the-client"
      wellKnownOpenIDConfigurationUrl: "https://oidc-provider.local/.well-known/openid-configuration"
```


## manual configuration

```yaml
jenkins:
  securityRealm:
    oic:
      clientId: "clientid-from-your-openid-provider"
      clientSecret: "the-secret-of-the-client"
      tokenServerUrl: "https://oidc-provider.local/token"
      authorizationServerUrl: "https://oidc-provider.local/auth"
      userInfoServerUrl: "https://oidc-provider.local/userinfo"
      userNameField: "preferred_username"
      tokenFieldToCheckKey: ""
      tokenFieldToCheckValue: ""
      fullNameFieldName: "name"
      emailFieldName: "email"
      groupsFieldName: "groups"
      simpleGroupsFieldName: "simple-group"
      nestedGroupFieldName: "nested-group"
      scopes: "web-origins address phone openid offline_access profile roles microprofile-jwt email"
      disableSslVerification: false
      logoutFromOpenidProvider: true
      endSessionEndpoint: "https://oidc-provider.local/logout"
      postLogoutRedirectUrl: "https://oidc-provider.local/logout"
      escapeHatchEnabled: false
```


