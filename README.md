# Jenkins Configuration as Code (a.k.a. JCasC) Plugin

[![Build Status](https://ci.jenkins.io/job/Plugins/job/configuration-as-code-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/configuration-as-code-plugin/job/master/)
[![Travis](https://img.shields.io/travis/jenkinsci/configuration-as-code-plugin.svg?logo=travis&label=build&logoColor=white)](https://travis-ci.org/jenkinsci/configuration-as-code-plugin)
[![Contributors](https://img.shields.io/github/contributors/jenkinsci/configuration-as-code-plugin.svg)](https://github.com/jenkinsci/configuration-as-code-plugin/graphs/contributors)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/1c872818b46f4fdd890e4a22af0bee8c)](https://www.codacy.com/app/casz/configuration-as-code-plugin)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/configuration-as-code.svg)](https://plugins.jenkins.io/configuration-as-code)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/configuration-as-code-plugin.svg?label=changelog)](https://github.com/jenkinsci/configuration-as-code-plugin/releases/latest)
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

<a name="handling-secrets"></a>

You can find more documentation about JCasC here:

- [Handling Secrets](./docs/features/secrets.adoc)
- [Exporting configurations](./docs/features/configExport.md)
- [Validating configurations](./docs/features/jsonSchema.md)

The configuration file format depends on the version of jenkins-core and installed plugins.
Documentation is generated from a live instance, as well as a JSON schema you can use to validate configuration file
with your favourite YAML tools.

The JSON Schema documentation can be found [here](./docs/features/jsonSchema.md).

**TODO**: Provide a Dockerfile to generate documentation from specified jenkins-core release and plugins.

## Installing plugins

We don't support installing plugins with JCasC you need to use something else for this,

Dockers users can use:\
[https://github.com/jenkinsci/docker/#preinstalling-plugins](https://github.com/jenkinsci/docker/#preinstalling-plugins)

Kubernetes users:\
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

- via Groovy script
  ```groovy
  import io.jenkins.plugins.casc.ConfigurationAsCode;
  ConfigurationAsCode.get().configure()
  ```

## Configuration-as-Code extension plugins

- [configuration-as-code-groovy-plugin](https://github.com/jenkinsci/configuration-as-code-groovy-plugin)\
  Allows to specify groovy code that should run on during configuration.
- [configuration-as-code-secret-ssm-plugin](https://github.com/jenkinsci/configuration-as-code-secret-ssm-plugin)\
  Allows to resolve secrets from AWS' SSM secrets
- [hashicorp-vault-plugin](https://github.com/jenkinsci/hashicorp-vault-plugin)\
  Allows to resolve secrets from Hashicorp vault


## Jenkins Enhancement Proposal

As configuration as code is demonstrated to be a highly requested topic in Jenkins community, we have published
[JEP 201](https://github.com/jenkinsci/jep/tree/master/jep/201) as proposal to make this a standard component
of the Jenkins project. The proposal was accepted. :tada:
