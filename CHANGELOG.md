# ChangeLog

## 1.9 (not released yet)

## 1.8

- [#753](https://github.com/jenkinsci/configuration-as-code-plugin/issues/753) Add support for recursive file search for CASC_JENKINS_CONFIG
- [#763](https://github.com/jenkinsci/configuration-as-code-plugin/issues/763) Introduce CASC_VAULT_PATHS to allow multiple vault paths to read from. CASC_VAULT_PATH kept for backwards compatibility and offering multi path too now.
- [#769](https://github.com/jenkinsci/configuration-as-code-plugin/issues/769) Remove plugin management beta feature - this feature was not widely used as it required restarts to be performed for plugins to be usable in a JCasC config file, moved proxy config and documented it
- [#770](https://github.com/jenkinsci/configuration-as-code-plugin/issues/770) Re-issue TTL expired vault tokens for user/pass and appRole/secret authentication. Refresh secrets from vault on each `configure()` call.

**Internal:**

- [#764](https://github.com/jenkinsci/configuration-as-code-plugin/issues/764) Add Vault Intergration test!

## 1.7

- fix revealing secret with default defined, where environment variable is defined, it would always default.
- [#746](https://github.com/jenkinsci/configuration-as-code-plugin/issues/746): add support for specifying vault engine version via CASC_VAULT_ENGINE_VERSION

## 1.6

- Make system environment variables available in the context used for running the jobdsl/groovy code defining the seed job.
- Add support for secrets while defining `jobs` declarations.
- [#688](https://github.com/jenkinsci/configuration-as-code-plugin/issues/688): fixed an IndexOutOfBounds exception
- Add support for Enterprise Vault to store secrets; set CASC_VAULT_NAMESPACE to provide a namespace
- many fixes to documentation and demos

## 1.5

- [#697](https://github.com/jenkinsci/configuration-as-code-plugin/issues/697): proper string interpolation for secrets.
- Improve explanation of CASC_VAULT in README.md

## 1.4

- Add support for Vault appRole authentication method

## 1.3

- fix regression configuring ssh private key from a secret source
- CLI command renamed as "reload-jcasc-configuration" to avoid conflict with core CLI
- add terraform demo
- restore support for k8s ConfigMaps mounts (don't recurse in CASC_JENKINS_CONFIG directory)
- added support for localisation
- added catching illegal arg exception when using ui form

## 1.2

- [SECURITY-929] Don't dump sensitive data in logs when configuring value
- [SECURITY] prevent directly entered private key to be exported in plain test
- fix and improve generated documentation
- use BulkChange to avoid repeated calls to save()
- list available attributes when unknown found in YAML to help diagnose mistakes
- log a warning when descriptor with unexpected design is detected

## 1.1

- [SECURITY-1124] Never export sensitive Secret
- fix plugin installation
- impersonate as SYSTEM to apply configuration
- removed Beta API annotations
- many fixes to documentation and demos

## 1.0

Initial public release

## pre-1.0

pre-1.0 alpha and release candidates changelog [on wiki](https://wiki.jenkins.io/display/JENKINS/Configuration+as+Code+Plugin)
