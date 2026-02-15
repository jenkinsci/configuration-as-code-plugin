# Using Vault to keep tabs on environment variables

## Prerequisites

- An instance of [Hashicorps Vault](https://www.vaultproject.io/) running
  - Using a docker-based approach is described [here](vault/setup-vault-using-docker.md)
- A token (or username and password) as credentials for accessing Vault
- [HashiCorp Vault plugin](https://github.com/jenkinsci/hashicorp-vault-plugin) v2.4.0+

## Put stuff into Vault

Creating content in Vault can be done either using curl (not recommended) or using Vault as a client.
A simple curl example can be found [here](vault/setup-vault-using-docker.md). Use a native vault application whenever possible. They are available for download [here](https://www.vaultproject.io/downloads.html). Using vault via docker requires you to be inside a running vault docker container. Otherwise the steps are the same.

The below example assumes that you have the following environment variables:

- `$VAULT_TOKEN` containing the token to use as access credentials
- `$VAULT_SERVER_URL` containing the URL to your vault server, i.e. `http://vault.domain.local:8200`

If needed, enter the docker container: `docker exec -it library/vault /bin/sh`

Execute the following to put data in vault.

```sh
vault write -address=$VAULT_SERVER_URL secret/jenkins/master \
AWS_ACCESS_KEY_ID="[Your AWS ACCESS KEY]" \
AWS_SECRET_ACCESS_KEY="[YOUR AWS SECRET ACCESS KEY]" \
SSH_PRIVATE_KEY=@/vault/file/secrets/jenkins_ssh_key
```

Essentially, anything can go into a vault, as long as it's KEY=VALUE formatted. If you work inside a docker container, the above example requires you to have the file `/vault/file/secrets/jenkins_ssh_key` exist inside the docker container.

## Usage

```bash
$ vault kv get  kv/jenkins/master
============= Data =============
Key                        Value
---                        -----
operator_pass        doggo
operator_userid      catto
```

and use them in configuration:

```yaml
# config truncated
credentials:
  system:
    domainCredentials:
      - credentials:
          - usernamePassword:
              scope: "GLOBAL"
              id: "${operator_userid}"
              username: "${operator_userid}"
              password: "${operator_pass}"
              description: "i am catto with doggo pass"
```

## Current limitations

Due to the dependency on BetterCloud's [vault-java-driver](https://github.com/BetterCloud/vault-java-driver), Vault's change default KV backend from v1 to v2 and HTTP endpoints change it's currently unable to use Vault's KV v2 secret store. ([see issue on BetterCloud project](https://github.com/BetterCloud/vault-java-driver/issues/114))
Be aware which version you use as default dev Vault server, starting from 0.10, it uses KV v2. [See docs](https://www.vaultproject.io/docs/secrets/kv/kv-v2.html)
