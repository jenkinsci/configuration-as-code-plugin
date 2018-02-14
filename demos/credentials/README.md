# credentials plugin

## sample configuration
            

```yaml
credentials:
  system:
    ? name: "test.com"
      description: "test.com domain"
      specifications:
        - hostnameSpecification:
            includes:
              - "*.test.com"
    : - usernamePassword:
          scope:    SYSTEM
          id:       sudo_password
          username: root
          password: ${SUDO_PASSWORD}
    ? # "global"
    : - certificate:
          scope:    SYSTEM
          id:       ssh_private_key
          password: ${SSH_KEY_PASSWORD}
          keyStoreSource: 
            fileOnMaster:
              keyStoreFile: /docker/secret/id_rsa

```

## implementation note

credentials plugin support relies on a custom adaptor component `CredentialsRootConfigurator`.

CredentialsStore uses as internal data model a `Map<Domain, List<Credentials>>`, so the yaml syntax (`? `) to define a 
complex mapping key. In previous sample, a Domain key is configured for `*.test.com` domain. 

Associated to this key, a list of credentials is defined based on hetero-describable symbol name (note the extra indent
after `usernamePassword` This guy is a single entry map 'usernamePassword' => map of attributes to build target type).

Credentials symbol name is inferred from implementation class simple name: `UsernamePasswordCredentialsImpl`
descriptor's clazz is `Credentials` 
we consider the `Impl` suffix as a common pattern to flag implementation class.
=> symbol name is `usernamePassword` 


## Examples

A list of some of the more common credentials. 

### SSH Credentials

Example that uses the [SSH credentials plugin](https://plugins.jenkins.io/ssh-credentials)

```yaml
credentials:
  system:
    ? name: "test.com"
      description: "test.com domain"
      specifications:
        - hostnameSpecification:
            includes:
              - "*.test.com"
    : - usernamePassword:
          scope:    SYSTEM
          id:       sudo_password
          username: root
          password: ${SUDO_PASSWORD}
    ? # "global"
      - basicSSHUserPrivateKey:
          scope: SYSTEM
          id: ssh_with_passprase
          username: ssh_root
          passphrase: ${SSH_KEY_PASSWORD}
          description: "SSH passphrase with private key file"
          privateKeySource:
            fileOnMaster:
              keyStoreFile: /docker/secret/id_rsa_2
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