# Jenkins Configuration as Code (a.k.a. JCasC) Plugin

[![Build Status](https://ci.jenkins.io/job/Plugins/job/configuration-as-code-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/configuration-as-code-plugin/job/master/)
[![Travis](https://img.shields.io/travis/jenkinsci/configuration-as-code-plugin.svg?logo=travis&label=build&logoColor=white)](https://travis-ci.org/jenkinsci/configuration-as-code-plugin)
[![Contributors](https://img.shields.io/github/contributors/jenkinsci/configuration-as-code-plugin.svg)](https://github.com/jenkinsci/configuration-as-code-plugin/graphs/contributors)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/1c872818b46f4fdd890e4a22af0bee8c)](https://www.codacy.com/app/casz/configuration-as-code-plugin)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/configuration-as-code.svg)](https://plugins.jenkins.io/configuration-as-code)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/configuration-as-code-plugin.svg?label=release)](https://github.com/jenkinsci/configuration-as-code-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/configuration-as-code.svg?color=blue)](https://plugins.jenkins.io/configuration-as-code)
[![Gitter](https://badges.gitter.im/jenkinsci/configuration-as-code-plugin.svg)](https://gitter.im/jenkinsci/configuration-as-code-plugin)

<img src="plugin/src/main/webapp/img/logo-head.svg" width="192">

## Introduction

Setting up Jenkins is a complex process, as both Jenkins and its plugins require some tuning and configuration,
with dozens of parameters to set within the web UI `manage` section.

Experienced Jenkins users rely on groovy init scripts to customize Jenkins and enforce desired state. Those
scripts directly invoke Jenkins API and as such can do everything (at your own risk). But they also require
you know Jenkins internals, and are confident in writing groovy scripts on top of Jenkins API.

The Configuration as Code plugin has been designed as an _**opinionated**_ way to configure Jenkins based on
human-readable declarative configuration files. Writing such a file should be feasible without being a Jenkins
expert, just translating into _code_ a configuration process one is used to executing in the web UI.

![configuration form](images/sample_form.png)

This plugin aims to replace above user interface based configuration with the below text based configuration.

```yaml
jenkins:
  securityRealm:
    ldap:
      configurations:
        - groupMembershipStrategy:
            fromUserRecord:
              attributeName: "memberOf"
          inhibitInferRootDN: false
          rootDN: "dc=acme,dc=org"
          server: "ldaps://ldap.acme.org:1636"
```

In addition, we want to have a well documented syntax file, and tooling to assist in writing and testing,
so end users have full guidance in using this tool set and do not have to search for examples on the Internet.

Also see the [presentation slides](https://docs.google.com/presentation/d/1VsvDuffinmxOjg0a7irhgJSRWpCzLg_Yskf7Fw7FpBg/edit?usp=sharing) from DevOps World - Jenkins World 2018 for overview.

## Getting Started

First, start a Jenkins instance with the [Configuration as Code](https://plugins.jenkins.io/configuration-as-code) plugin installed.

- Those running Jenkins as a [Docker](https://github.com/jenkinsci/docker) container (and maybe also [pre-installing plugins](https://github.com/jenkinsci/docker#preinstalling-plugins)), do include [Configuration as Code](https://plugins.jenkins.io/configuration-as-code) plugin.

Second, the plugin looks for the `CASC_JENKINS_CONFIG` environment variable. The variable can point to any of the following:

- Path to a folder containing a set of config files. For example, `/var/jenkins_home/casc_configs`.
- A full path to a single file. For example, `/var/jenkins_home/casc_configs/jenkins.yaml`.
- A URL pointing to a file served on the web. For example, `https://acme.org/jenkins.yaml`.

If `CASC_JENKINS_CONFIG` points to a folder, the plugin will recursively traverse the folder to find file (suffix with .yml,.yaml,.YAML,.YML), but doesn't contain hidden files or hidden subdirectories. It doesn't follow symbolic links.

If you do not set the `CASC_JENKINS_CONFIG` environment variable, the plugin will
default to looking for a single config file in `$JENKINS_ROOT/jenkins.yaml`.

If everything was setup correctly, you should now be able to browse the Configuration as Code page with `Manage Jenkins` -> `Configuration as Code`.

## Run Locally

Prerequisites: _Java_, _Maven_ & _IntelliJ IDEA_

- Ensure Java 8 is available. There are unresolved issues with Java 10/11 as of October 24, 2018.

  ```shell
  /usr/libexec/java_home
  ```

  ```text
  /Library/Java/JavaVirtualMachines/jdk-11.0.1.jdk/Contents/Home
  ```

  - If Java 11 is selected by default, check other available Java version below.

  ```shell
  /usr/libexec/java_home --verbose
  ```

  ```text
  Matching Java Virtual Machines (3):
      11.0.1, x86_64: "Java SE 11.0.1"  /Library/Java/JavaVirtualMachines/jdk-11.0.1.jdk/Contents/Home
      10.0.2, x86_64: "Java SE 10.0.2"  /Library/Java/JavaVirtualMachines/jdk-10.0.2.jdk/Contents/Home
      1.8.0_192, x86_64:  "Java SE 8"  /Library/Java/JavaVirtualMachines/jdk1.8.0_192.jdk/Contents/Home

  /Library/Java/JavaVirtualMachines/jdk-11.0.1.jdk/Contents/Home
  ```

  - Use the alternate Java 8.

  ```shell
  export JAVA_HOME=`/usr/libexec/java_home -v 1.8`
  echo $JAVA_HOME
  ```

  ```text
  /Library/Java/JavaVirtualMachines/jdk1.8.0_192.jdk/Contents/Home
  ```

- Ensure Maven is included in the PATH environment variable.

  ```shell
  export PATH=$PATH:/path/to/apache-maven-x.y.z/bin
  ```

### IntelliJ IDEA

- Open the root directory of this project in IntelliJ IDEA.
- If you are opening the first time, wait patiently while project dependencies are being downloaded.
- Click `Run` in the menu. Select `Edit Configurations` in the menu item.
- Click `Add New Configuration` (`+`) in the top left of the shown dialog. Select `Maven`.
- Under `Parameters` tab group, `Working directory:` is `/path/to/configuration-as-code-plugin/plugin`.
- Under `Parameters` tab group, `Command line:` is `hpi:run`.
- Verify that IntelliJ IDEA is not using bundled maven.
  - Click `File` -> `Preferences...` -> `Build, Execution, Deployment` -> `Build Tools` -> `Maven`.
  - `Maven home directory:` has `/path/to/apache-maven-x.y.z` value, not `Bundled (Maven 3)`.
- Open <http://localhost:8080/jenkins/configuration-as-code/> to test the plugin locally.

### CLI

- Go into the `plugin` child directory under the root directory of this project.
- Use the below commands.

```shell
export JAVA_HOME=`/usr/libexec/java_home -v 1.8`
export PATH=$PATH:/path/to/apache-maven-x.y.z/bin
mvn hpi:run
```

```text
...
INFO: Jenkins is fully up and running
```

- Open <http://localhost:8080/jenkins/configuration-as-code/> to test the plugin locally.

## Initial Configuration

When configuring the first Jenkins instance, browse the examples shown in the [demos](demos)
directory of this repository. If you have a plugin that does not have an example, consult the reference
help document. Click the `Documentation` link at the bottom of the Configuration as Code page.

![Reference Page](images/reference.png)

If you want to configure a specific plugin, search the page for the name of the plugin. The page will
show you which root element belongs to the configuration. Most installed plugins belong under the `unclassified` root
element.

![Unclassified Section](images/unclassified.png)

## Examples

This configuration file includes root entries for various components of your primary Jenkins installation. The `jenkins` one is for the root Jenkins object, and other ones are for various global configuration elements.

```yaml
jenkins:
  securityRealm:
    ldap:
      configurations:
        - groupMembershipStrategy:
            fromUserRecord:
              attributeName: "memberOf"
          inhibitInferRootDN: false
          rootDN: "dc=acme,dc=org"
          server: "ldaps://ldap.acme.org:1636"

  nodes:
    - permanent:
        name: "static-agent"
        remoteFS: "/home/jenkins"
        launcher:
          jnlp:

  slaveAgentPort: 50000
  agentProtocols:
    - "jnlp2"
tool:
  git:
    installations:
      - name: git
        home: /usr/local/bin/git
unclassified:
  mailer:
    adminAddress: admin@acme.org
    replyToAddress: do-not-reply@acme.org
    # Note that this does not work right now
    #smtpHost: smtp.acme.org
    smtpPort: 4441
credentials:
  system:
    domainCredentials:
      credentials:
        - certificate:
            scope: SYSTEM
            id: ssh_private_key
            keyStoreSource:
              fileOnMaster:
                keyStoreFile: /docker/secret/id_rsa
```

Also see [demos](demos) folder with various samples.

## Documentation

The configuration file format depends on the version of jenkins-core and installed plugins.
Documentation is generated from a live instance, as well as a JSON schema you can use to validate configuration file
with your favourite YAML tools.

## Handling Secrets

Currently, you can provide initial secrets to JCasC that all rely on <key,value>
substitution of strings in the configuration. For example, `Jenkins: "${some_var}"`. Default variable substitution
using the `:-` operator from `bash` is also available. For example, `key: "${VALUE:-defaultvalue}"` will evaluate to `defaultvalue` if `$VALUE` is unset. To escape a string from secret interpolation, put `^` in front of the value. For example, `Jenkins: "^${some_var}"` will produce the literal `Jenkins: "${some_var}"`.

## Secret sources

We can provide these initial secrets in the following ways:

- Using environment variables.
- Using docker-secrets, where files on path `/run/secrets/${KEY}` will be replaced by `${KEY}` in the configuration. The base folder `/run/secrets` can be overridden by setting the environment variable `SECRETS`. So this can be used as a file based secret, and not just docker secrets.
- Using Kubernetes secrets, logic is the same as for docker-secrets. The secret needs to be mounted as a file to `/run/secrets/`, and then the filename can be used as the KEY. For example:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: secret-name
data:
  filename: {{ "encoded string" | b64enc }}
```

can be used as:

```yaml
- credentials:
    - string:
      id: "cred-id"
      secret: ${filename}
```

- Using Vault, see below.

### Security and compatibility considerations

<!-- TODO(oleg_nenashev): Add a link to the advisory once ready -->

Jenkins configurations might include property definitions,
e.g. for Token Macro resolution in Mail Ext Plugin.
Such properties are not supposed to be resolved when importing configurations,
but the JCasC plugin has no way to determine which variables should be resolved when reading the configurations.

In some cases non-admin users can contribute to JCasC exports if they have some permissions
(e.g. agent/view configuration or credentials management),
and they could potentially inject variable expressions in plain text fields like descriptions 
and then see the resolved secrets in Jenkins Web UI if the Jenkins admin exports and imports the configuration without checking contents.
It led to a security vulnerability which was addressed in JCasC `1.25` (SECURITY-1446).

- When reading configuration YAMLs, JCasC plugin will try to resolve
  **all** variables having the `${VARNAME}` format.
- Starting from JCasC `1.25`, JCasC export escapes the internal variable expressions,
  e.g. as `^${VARNAME}`, so newly exported and then imported configurations are 
  are not subject for this risk
- For previously exported configurations, Jenkins admins are expected to manually
  resolve the issues by putting the escape symbol `^` in front of variables which should not be resolved 

### Vault

Prerequisites: [HashiCorp Vault plugin](https://github.com/jenkinsci/hashicorp-vault-plugin) v2.4.0+

- The environment variable `CASC_VAULT_PW` must be present, if token is not used and appRole/Secret is not used. (Vault password.)
- The environment variable `CASC_VAULT_USER` must be present, if token is not used and appRole/Secret is not used. (Vault username.)
- The environment variable `CASC_VAULT_APPROLE` must be present, if token is not used and U/P not used. (Vault AppRole ID.)
- The environment variable `CASC_VAULT_APPROLE_SECRET` must be present, it token is not used and U/P not used. (Vault AppRole Secret ID.)
- The environment variable `CASC_VAULT_TOKEN` must be present, if U/P is not used. (Vault token.)
- The environment variable `CASC_VAULT_PATHS` must be present. (Comma separated vault key paths. For example, `secret/jenkins,secret/admin`.)
- The environment variable `CASC_VAULT_URL` must be present. (Vault url, including port number.)
- The environment variable `CASC_VAULT_MOUNT` is optional. (Vault auth mount. For example, `ldap` or another username & password authentication type, defaults to `userpass`.)
- The environment variable `CASC_VAULT_NAMESPACE` is optional. If used, sets the Vault namespace for Enterprise Vaults.
- The environment variable `CASC_VAULT_FILE` is optional, provides a way for the other variables to be read from a file instead of environment variables.
- The environment variable `CASC_VAULT_ENGINE_VERSION` is optional. If unset, your vault path is assumed to be using kv version 2. If your vault path uses engine version 1, set this variable to `1`.
- The issued token should have read access to vault path `auth/token/lookup-self` in order to determine its expiration time. JCasC will re-issue a token if its expiration is reached (except for `CASC_VAULT_TOKEN`).

If the environment variables `CASC_VAULT_URL` and `CASC_VAULT_PATHS` are present, JCasC will try to gather initial secrets from Vault. However for it to work properly there is a need for authentication by either the combination of `CASC_VAULT_USER` and `CASC_VAULT_PW`, a `CASC_VAULT_TOKEN`, or the combination of `CASC_VAULT_APPROLE` and `CASC_VAULT_APPROLE_SECRET`. The authenticated user must have at least read access.

You can also provide a `CASC_VAULT_FILE` environment variable where you load the secrets from a file.

File should be in a Java Properties format

```properties
CASC_VAULT_PW=PASSWORD
CASC_VAULT_USER=USER
CASC_VAULT_TOKEN=TOKEN
CASC_VAULT_PATHS=secret/jenkins/master,secret/admin
CASC_VAULT_URL=https://vault.dot.com
CASC_VAULT_MOUNT=ldap
```

A good use for `CASC_VAULT_FILE` would be together with docker secrets.

```yaml
version: "3.6"

services:
  jenkins:
    environment:
      CASC_VAULT_FILE: /run/secrets/jcasc_vault
    restart: always
    build: .
    image: jenkins.master:v1.0
    ports:
      - 8080:8080
      - 50000:50000
    volumes:
      - jenkins-home:/var/jenkins_home
    secrets:
      - jcasc_vault

volumes:
  jenkins-home:

secrets:
  jcasc_vault:
    file: ./secrets/jcasc_vault
```

**TODO**: Provide a Dockerfile to generate documentation from specified jenkins-core release and plugins.

## Installing plugins

We don't support installing plugins with JCasC you need to use something else for this,

Dockers users can use:  
[https://github.com/jenkinsci/docker/#preinstalling-plugins](https://github.com/jenkinsci/docker/#preinstalling-plugins)

Kubernetes users:  
[https://github.com/helm/charts/tree/master/stable/jenkins](https://github.com/helm/charts/tree/master/stable/jenkins)

## Supported Plugins

Most plugins should be supported out-of-the-box, or maybe require some minimal changes. See this [dashboard](https://issues.jenkins.io/secure/Dashboard.jspa?selectPageId=18341) for known compatibility issues.

## Triggering Configuration Reload

You have the following option to trigger a configuration reload:

- via the user interface: `Manage Jenkins -> Configuration -> Reload existing configuration`
- via http POST to `JENKINS_URL/configuration-as-code/reload`
  Note: this needs to include a valid CRUMB and authentication information e.g. username + token of a user with admin
  permissions. Since Jenkins 2.96 CRUMB is not needed for API tokens.
- via Jenkins CLI
- via http POST to `JENKINS_URL/reload-configuration-as-code`
  It's disabled by default and secured via a token configured as system property `casc.reload.token`.
  Setting the system property enables this functionality and the requests need to include the token as
  query parameter named `casc-reload-token`, i.e. `JENKINS_URL/reload-configuration-as-code/?casc-reload-token=32424324rdsadsa`.

  `curl  -X POST "JENKINS_URL:8080/reload-configuration-as-code/?casc-reload-token=32424324rdsadsa"`
  
## Configuration-as-Code extension plugins

- [configuration-as-code-groovy-plugin](https://github.com/jenkinsci/configuration-as-code-groovy-plugin)
  Allows to specify groovy code that should run on during configuration.
- [configuration-as-code-secret-ssm-plugin](https://github.com/jenkinsci/configuration-as-code-secret-ssm-plugin)
  Allows to resolve secrets from AWS' SSM secrets
- [hashicorp-vault-plugin](https://github.com/jenkinsci/hashicorp-vault-plugin)
  Allows to resolve secrets from Hashicorp vault
  

## Jenkins Enhancement Proposal

As configuration as code is demonstrated to be a highly requested topic in Jenkins community, we have published
[JEP 201](https://github.com/jenkinsci/jep/tree/master/jep/201) as proposal to make this a standard component
of the Jenkins project. The proposal was accepted. :tada:
