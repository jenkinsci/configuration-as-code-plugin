# credentials plugin

Requires `credentials` >= 2.2.0

All values with `"${SOME_SECRET}"` is resolved by our Secret Sources Resolver you can [read more about which sources are supported](../../docs/features/secrets.adoc#secret-sources)

Since JCasC version v1.42 we have added support for variable expansion for `base64`, `readFileBase64` and `readFile`.
More variable expansion options have been added later.
[Read more about the syntax](../../docs/features/secrets.adoc#passing-secrets-through-variables)

You can also see an [example below](#example)

## Sample Configuration

```yaml
credentials:
  system:
    domainCredentials:
      - domain:
          name: "test.com"
          description: "test.com domain"
          specifications:
            - hostnameSpecification:
                includes: "*.test.com"
        credentials:
          - usernamePassword:
              scope: SYSTEM
              id: sudo_password
              username: root
              password: "${SUDO_PASSWORD}"
```

## Example

```yaml
jenkins:
  systemMessage: "Example of configuring credentials in Jenkins"

credentials:
  system:
    domainCredentials:
      - credentials:
          - basicSSHUserPrivateKey:
              scope: SYSTEM
              id: ssh_with_passphrase_provided
              username: ssh_root
              passphrase: ${SSH_KEY_PASSWORD}
              description: "SSH passphrase with private key file. Private key provided"
              privateKeySource:
                directEntry:
                  privateKey: "${SSH_PRIVATE_KEY}"
          # Another option passing via a file via ${readFile:/path/to/file}
          - basicSSHUserPrivateKey:
              scope: SYSTEM
              id: ssh_with_passphrase_provided_via_file
              username: ssh_root
              passphrase: "${SSH_KEY_PASSWORD}"
              description: "SSH passphrase with private key file. Private key provided"
              privateKeySource:
                directEntry:
                  privateKey: "${readFile:${SSH_PRIVATE_FILE_PATH}}" # Path to file loaded from Environment Variable
          - usernamePassword:
              scope: GLOBAL
              id: "username"
              username: "some-user"
              password: "${SOME_USER_PASSWORD}"
              description: "Username/Password Credentials for some-user"
          - string:
              scope: GLOBAL
              id: "secret-text"
              secret: "${SECRET_TEXT}"
              description: "Secret Text"
          - aws:
              scope: GLOBAL
              id: "AWS"
              accessKey: "${AWS_ACCESS_KEY}"
              secretKey: "${AWS_SECRET_ACCESS_KEY}"
              description: "AWS Credentials"
          - file:
              scope: GLOBAL
              id: "secret-file"
              fileName: "mysecretfile.txt"
              secretBytes: "${base64:${readFile:${SECRET_FILE_PATH}}}" # secretBytes requires base64 encoded content
          - file:
              scope: GLOBAL
              id: "secret-file_via_binary_file"
              fileName: "mysecretfile.txt"
              secretBytes: "${readFileBase64:${SECRET_FILE_PATH}}" # secretBytes requires base64 encoded content
          - certificate:
              scope: GLOBAL
              id: "secret-certificate"
              password: "${SECRET_PASSWORD_CERT}"
              description: "my secret cert"
              keyStoreSource:
                uploaded:
                  uploadedKeystore: "${readFileBase64:${SECRET_CERT_FILE_PATH}}" # uploadedKeystore requires BINARY base64 encoded content
```

## implementation note

Credentials plugin support relies on a custom adaptor components `CredentialsRootConfigurator` and `SystemCredentialsProviderConfigurator`.

Global credentials can be registered by just not providing a domain (i.e `null`).

Credentials symbol name is inferred from implementation class simple name: `UsernamePasswordCredentialsImpl`
descriptor's clazz is `Credentials`
we consider the `Impl` suffix as a common pattern to flag implementation class.
=> symbol name is `usernamePassword`
