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


- [Introduction](#introduction)
- [Getting Started](#getting-started)
- [Examples and demos](./demos)
- [Handling Secrets](./docs/features/secrets.adoc)
- [Exporting configurations](./docs/features/configExport.md)
- [Validating configurations](./docs/features/jsonSchema.md)
- [Triggering Configuration Reload](./docs/features/configurationReload.md)
- [Installing plugins](#installing-plugins)
- [Supported Plugins](#supported-plugins)
- [Adding JCasC support to a plugin](#adding-jCasC-support-to-a-plugin)
- [Configuration-as-Code extension plugins](#configuration-as-Code-extension-plugins)
- [Jenkins Enhancement Proposal](#jenkins-enhancement-proposal)

## Introduction

Setting up Jenkins is a complex process, as both Jenkins and its plugins require some tuning and configuration,
with dozens of parameters to set within the web UI `manage` section.

Experienced Jenkins users rely on groovy init scripts to customize Jenkins and enforce the desired state. Those
scripts directly invoke Jenkins API and, as such, can do everything (at your own risk). But they also require
you to know Jenkins internals and are confident in writing groovy scripts on top of Jenkins API.

The Configuration as Code plugin is an _**opinionated**_ way to configure Jenkins based on
human-readable declarative configuration files. Writing such a file should be feasible without being a Jenkins
expert, just translating into _code_ a configuration process one is used to executing in the web UI.

The below configuration file includes root entries for various components of your primary Jenkins installation. The `jenkins` one is for the root Jenkins object, and the other ones are for different global configuration elements.

```yaml
jenkins:
  systemMessage: "Jenkins configured automatically by Jenkins Configuration as Code plugin\n\n"
  globalNodeProperties:
  - envVars:
      env:
      - key: VARIABLE1
        value: foo
      - key: VARIABLE2
        value: bar
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
            workDirSettings:
              disabled: true
              failIfWorkDirIsMissing: false
              internalDir: "remoting"
              workDirPath: "/tmp"

  slaveAgentPort: 50000
  agentProtocols:
    - "jnlp2"

tool:
  git:
    installations:
      - name: git
        home: /usr/local/bin/git

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
                  privateKey: ${SSH_PRIVATE_KEY}
```

Additionally, we want to have a well-documented syntax file and tooling to assist in writing and testing,
so end users have full guidance in using this toolset and do not have to search for examples on the Internet.

See the [presentation slides](https://docs.google.com/presentation/d/1VsvDuffinmxOjg0a7irhgJSRWpCzLg_Yskf7Fw7FpBg/edit?usp=sharing) from DevOps World - Jenkins World 2018 for an overview.

## Getting Started

First, start a Jenkins instance with the [Configuration as Code](https://plugins.jenkins.io/configuration-as-code) plugin installed.

- Those running Jenkins as a [Docker](https://github.com/jenkinsci/docker) container (and maybe also [pre-installing plugins](https://github.com/jenkinsci/docker#preinstalling-plugins)), do include [Configuration as Code](https://plugins.jenkins.io/configuration-as-code) plugin.

Second, the plugin looks for the `CASC_JENKINS_CONFIG` environment variable. The variable points to a comma-separated list of any of the following:

- Path to a folder containing a set of config files. For example, `/var/jenkins_home/casc_configs`.
- A full path to a single file. For example, `/var/jenkins_home/casc_configs/jenkins.yaml`.
- A URL pointing to a file served on the web. For example, `https://acme.org/jenkins.yaml`.

If an element of `CASC_JENKINS_CONFIG` points to a folder, the plugin will recursively traverse the folder to find file(s) with .yml,.yaml,.YAML,.YML suffix. It will exclude hidden files or files that contain a hidden folder in **any part** of the full path. It follows symbolic links for both files and directories.
<details><summary>Exclusion examples</summary>

`CASC_JENKINS_CONFIG=/jenkins/casc_configs`  
:heavy_check_mark: `/jenkins/casc_configs/jenkins.yaml`  
:heavy_check_mark: `/jenkins/casc_configs/dir1/config.yaml`  
:x: `/jenkins/casc_configs/.dir1/config.yaml`  
:x: `/jenkins/casc_configs/..dir2/config.yaml`  
  
`CASC_JENKINS_CONFIG=/jenkins/.configs/casc_configs` contains hidden folder `.config`  
:x: `/jenkins/.configs/casc_configs/jenkins.yaml`  
:x: `/jenkins/.configs/casc_configs/dir1/config.yaml`  
:x: `/jenkins/.configs/casc_configs/.dir1/config.yaml`  
:x: `/jenkins/.configs/casc_configs/..dir2/config.yaml`  
</details>

All configuration files that are discovered MUST be supplementary. They cannot overwrite each other's configuration values. This creates a conflict and raises a `ConfiguratorException`. Thus, the order of traversal does not matter to the final outcome.

Instead of setting the `CASC_JENKINS_CONFIG` environment variable, you can also define using
the `casc.jenkins.config` Java property.  This is useful when installing Jenkins via a package
management tool and can't set an environment variable outside of a package-managed file, which could
be overwritten by an update.  For RHEL/CentOS systems, you can append the following to the
`JENKINS_JAVA_OPTIONS` entry in `/etc/sysconfig/jenkins`
 
  `-Dcasc.jenkins.config=/jenkins/casc_configs`
 
If you do not set the `CASC_JENKINS_CONFIG` environment variable or the `casc.jenkins.config` Java
property, the plugin will default to looking for a single config file in
`$JENKINS_HOME/jenkins.yaml`.

If set up correctly, you should be able to browse the Configuration as Code page `Manage Jenkins` -> `Configuration as Code`.

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

See [demos](demos) folder with various samples.

### LDAP

Replace user interface based configuration for LDAP with the text-based configuration.

![configuration form](images/sample_form.png)

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

### Yaml Aliases and Anchors

Replace repeated elements with yaml anchors.
Anchor keys must be prefixed with `x-` due to JCasC handling unknown root elements.

```yaml
x-jenkins-linux-node: &jenkins_linux_node_anchor
  remoteFS: "/home/jenkins"
  launcher:
    jnlp:
      workDirSettings:
        disabled: true
        failIfWorkDirIsMissing: false
        internalDir: "remoting"
        workDirPath: "/tmp"

jenkins:
  nodes:
    - permanent:
        name: "static-agent1"
        <<: *jenkins_linux_node_anchor
    - permanent:
        name: "static-agent2"
        <<: *jenkins_linux_node_anchor
```

Which produces two permanent agent nodes which can also be written like this.

```yaml
jenkins:
  nodes:
    - permanent:
        name: "static-agent1"
        remoteFS: "/home/jenkins"
        launcher:
          jnlp:
            workDirSettings:
              disabled: true
              failIfWorkDirIsMissing: false
              internalDir: "remoting"
              workDirPath: "/tmp"
    - permanent:
        name: "static-agent2"
        remoteFS: "/home/jenkins"
        launcher:
          jnlp:
            workDirSettings:
              disabled: true
              failIfWorkDirIsMissing: false
              internalDir: "remoting"
              workDirPath: "/tmp"
```


## Installing plugins

We don't support installing plugins with JCasC, so you need to use something else for this,

Dockers users can use:\
[https://github.com/jenkinsci/docker/#preinstalling-plugins](https://github.com/jenkinsci/docker/#preinstalling-plugins)

Kubernetes users:\
[https://github.com/jenkinsci/helm-charts](https://github.com/jenkinsci/helm-charts)

## Supported Plugins

Most plugins should be supported out-of-the-box or maybe require some minimal changes. See this [dashboard](https://issues.jenkins.io/secure/Dashboard.jspa?selectPageId=18341) for known compatibility issues.

## Adding JCasC support to a plugin

Plugin developers wanting to support JCasC in their plugin should [check out our how-to guide](docs/PLUGINS.md).

## Configuration-as-Code extension plugins

- [configuration-as-code-groovy-plugin](https://github.com/jenkinsci/configuration-as-code-groovy-plugin)\
  Allows specifying groovy code that should run on during configuration.

## Jenkins Enhancement Proposal

As configuration as code is demonstrated to be a highly requested topic in the Jenkins community, we have published
[JEP 201](https://github.com/jenkinsci/jep/tree/master/jep/201) as a proposal to make this a standard component
of the Jenkins project. The proposal was accepted. :tada:
