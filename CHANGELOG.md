# ChangeLog

## 1.4 (not released yet)
- Add support for Vault appRole authentication method
- Add proper string interpolation for secrets.

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
- list available attributes when unknown found in yaml to help diagnose mistakes
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
