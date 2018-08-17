# credentials plugin

## sample configuration

```yaml
credentials:
  system:
    domainCredentials:
      - domain :
          name: "test.com"
          description: "test.com domain"
          specifications:
            - hostnameSpecification:
                includes: "*.test.com"
        credentials:
          - usernamePassword:
              scope:    SYSTEM
              id:       sudo_password
              username: root
              password: ${SUDO_PASSWORD}

```

## implementation note

Credentials plugin support relies on a custom adaptor components `CredentialsRootConfigurator` and `SystemCredentialsProviderConfigurator`.

Global credentials can be registered by just not providing a domain (i.e `null`).

Credentials symbol name is inferred from implementation class simple name: `UsernamePasswordCredentialsImpl`
descriptor's clazz is `Credentials`
we consider the `Impl` suffix as a common pattern to flag implementation class.
=> symbol name is `usernamePassword`

## Examples

A list of some of the more common credentials.

### SSH Credentials

Example that uses the [SSH credentials plugin](https://plugins.jenkins.io/ssh-credentials).

As of version 1.14, it is no longer possible to load a ssh key from a file. It has been deprecated due to [CVE-2018-1000601](https://jenkins.io/security/advisory/2018-06-25/#SECURITY-440).

```yaml
credentials:
  system:
    domainCredentials:
      - credentials:
          - basicSSHUserPrivateKey:
              scope: SYSTEM
              id: ssh_with_passprase_provided
              username: ssh_root
              passphrase: ${SSH_KEY_PASSWORD}
              description: "SSH passphrase with private key file. Private key provided"
              privateKeySource:
                directEntry:
                  privateKey: ${SSH_PRIVATE_KEY}
```
