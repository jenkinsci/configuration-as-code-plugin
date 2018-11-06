# Jenkins Configuration as Code Plugin

[![Build Status](https://ci.jenkins.io/job/Plugins/job/configuration-as-code-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/configuration-as-code-plugin/job/master/)
[![Jenkins Plugins](https://img.shields.io/jenkins/plugin/v/configuration-as-code.svg)](https://plugins.jenkins.io/configuration-as-code)
[![Gitter](https://badges.gitter.im/jenkinsci/configuration-as-code-plugin.svg)](https://gitter.im/jenkinsci/configuration-as-code-plugin)

<img src="plugin/src/main/webapp/img/logo-head.svg" width="192">

View the [wiki](https://wiki.jenkins.io/display/JENKINS/configuration+as+code+plugin) page. See [presentation slides](https://docs.google.com/presentation/d/1VsvDuffinmxOjg0a7irhgJSRWpCzLg_Yskf7Fw7FpBg/edit?usp=sharing) from Jenkins World 2018.

Join our Jenkins Configuration as Code (JCasC) office hours meeting scheduled for every second Wednesday. Use the Hangout on Air link from our [Gitter](https://gitter.im/jenkinsci/configuration-as-code-plugin) chat channel. As an alternative, use the link from the [invitation](https://calendar.google.com/event?action=TEMPLATE&tmeid=MmdwdTE1cTFvaGw1NGUycGxqdWUwcXExaWFfMjAxODA3MjVUMDcwMDAwWiBld2VAcHJhcW1hLm5ldA&tmsrc=ewe%40praqma.net&scp=ALL). See previous [meeting minutes](https://docs.google.com/document/d/1Hm07Q1egWL6VVAqNgu27bcMnqNZhYJmXKRvknVw4Y84/edit?usp=sharing).

## Introduction

Setting up Jenkins is a complex process, as both Jenkins and its plugins require some tuning and configuration,
with dozens of parameters to set within the web UI `manage` section.

Experienced Jenkins users rely on groovy init scripts to customize Jenkins and enforce desired state. Those
scripts directly invoke Jenkins API and as such can do everything (at your own risk). But they also require
you know Jenkins internals, and are confident in writing groovy scripts on top of Jenkins API.

Configuration-as-Code plugin has been designed as an _**opinionated**_ way to configure Jenkins based on
human-readable declarative configuration files. Writing such a file should be feasible without being a Jenkins
expert, just translating into _code_ a configuration process one is used to executing in the web UI.

![configuration form](images/sample_form.png)

This plugin aims to replace above user-interface based configuration with the below text based configuration.

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
so end-users have full guidance in using this tool-set and do not have to search for examples on the Internet.

## Getting Started

First, start a Jenkins instance with JCasC [plugin](https://plugins.jenkins.io/configuration-as-code) installed.

- Those running Jenkins as a [Docker container](https://github.com/jenkinsci/docker) (and maybe also [pre-installing plugins](https://github.com/jenkinsci/docker#preinstalling-plugins)), do include [configuration-as-code](https://plugins.jenkins.io/configuration-as-code) plugin.

Second, the plugin requires the `CASC_JENKINS_CONFIG` environment variable to exist. The variable can point to the following:

- Path to a folder containing a set of config files. For example, `/var/jenkins_home/casc_configs`.
- A full path to a single file. For example, `/var/jenkins_home/casc_configs/jenkins.yaml`.
- A URL pointing to a file served on the web. For example, `https://acme.org/jenkins.yaml`.

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
- Open http://localhost:8080/jenkins/configuration-as-code/ to test the plugin locally.

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
...
INFO: Jenkins is fully up and running
```

- Open http://localhost:8080/jenkins/configuration-as-code/ to test the plugin locally.

## Initial Configuration

When configure the first Jenkins instance, browse the examples shown in the [demos](demos)
directory of this repository. If you have a plugin that do not have an example, consult the reference
help document. Click the `Documentation` link at the bottom of the Configuration as Code page.

![Reference Page](images/reference.png)

If you might wish to configure a specific plugin, search the page for the name of the plugin. The page will
show you which root element belongs to the configuration. Most installed plugins belong under the `unclassified` root
element.

![Unclassified Section](images/unclassified.png)

## Examples

This configuration file includes root entries for various components of your primary Jenkins installation. The `jenkins` one is for the root Jenkins object, and other ones are for various global configuration elements.

```yaml
jenkins:
  securityRealm:
    (...)

  nodes:
    - permanent:
        name: "static-slave"
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
Documentation is generated from a live instance, as well as a JSON-schema you can use to validate configuration file
with your favourite yaml tools.

## Handling Secrets

Currently, you can provide initial secrets to Configuration-as-Code that all rely on <key,value>
substitution of strings in configuration. For example, ``Jenkins: `${some_var}` ``. Default variable substitution
using the `:-` operator from `bash` is also available. For example, `key: ${VALUE:-defaultvalue}` will evaluate to `defaultvalue` if `$VALUE` is unset.

We can provide these initial secrets in the following ways:

- Using environment variables.
- Using docker-secrets, where files on path `/run/secrets/${KEY}` will be replaced by `${KEY}` in configuration. The base folder `/run/secrets` can be overriden by setting the environment variable `SECRETS`. So this can be used as a file based secret, and not just docker secrets.
- Using Kubernetes secrets, logic is the same as for docker-secrets. The secret needs to be mounted as a file to `/run/secrets/`, and then the filename can be used as the KEY. For example:
```
apiVersion: v1
kind: Secret
metadata:
  name: secret-name
data:
  filename: {{ "encoded string" | b64enc }}
```
can be used as:
```
- credentials:
  - string:
    id: "cred-id"
    secret: ${filename}
```
- Using Vault, see following section.

### Vault

Prerequisites:

- The environment variable `CASC_VAULT_PW` must be present, if token is not used and appRole/Secret is not used. (Vault password.)
- The environment variable `CASC_VAULT_USER` must be present, if token is not used and appRole/Secret is not used. (Vault username.)
- The environment variable `CASC_VAULT_APPROLE` must be present, if token is not used and U/P not used. (Vault AppRole ID.)
- The environment variable `CASC_VAULT_APPROLE_SECRET` must be present, it token is not used and U/P not used. (Value AppRole Secret ID.)
- The environment variable `CASC_VAULT_TOKEN` must be present, if U/P is not used. (Vault token.)
- The environment variable `CASC_VAULT_PATH` must be present. (Vault key path. For example, `/secrets/jenkins`.)
- The environment variable `CASC_VAULT_URL` must be present. (Vault url, including port number.)
- The environment variable `CASC_VAULT_MOUNT` is optional. (Vault auth mount. For example, `ldap` or another username & password authentication type, defaults to `userpass`.)
- The environment variable `CASC_VAULT_FILE` is optional, provides a way for the other variables to be read from a file instead of environment variables.

If all those 4 are present, Configuration-as-Code will try to gather initial secrets from Vault. Requires read access for the configured user.

You can also provide a `CASC_VAULT_FILE` environment variable where you load the secrets from file.

File should be in a Java Properties format

```properties
CASC_VAULT_PW=PASSWORD
CASC_VAULT_USER=USER
CASC_VAULT_TOKEN=TOKEN
CASC_VAULT_PATH=secret/jenkins/master
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

## Plugin Management

Status: `BETA`

We currently do support plugin installation but it will remain in `beta` for the foreseeable future. Generally
we recommend that you package your plugins with your Jenkins distribution as plugin installation often requires a
restart and can cause problems with plugin dependencies. So if you want to try it, you can.

Current implementation do require a restart if you add a plugin.

Example: (Requires Configuration as Code Plugin version > 0.7-alpha)

```yaml
plugins:
  required:
    git: 3.9.0
    warnings: 4.67
```

## Supported Plugins

Most plugins should be supported out-of-the-box, or maybe require some minimal changes. See this [dashboard](https://issues.jenkins-ci.org/secure/Dashboard.jspa?selectPageId=17346) for known compatibility issues.

## Jenkins Enhancement Proposal

As Configuration-as-code is demonstrated to be a highly requested topic in Jenkins community, we have published
[JEP 201](https://github.com/jenkinsci/jep/tree/master/jep/201) as proposal to make this a standard component
of the Jenkins project. The proposal was accepted. :tada:
