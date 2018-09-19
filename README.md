[![Build Status](https://ci.jenkins.io/job/Plugins/job/configuration-as-code-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/configuration-as-code-plugin/job/master/)

Join the community and ask questions
[![Gitter](https://badges.gitter.im/jenkinsci/configuration-as-code-plugin.svg)](https://gitter.im/jenkinsci/configuration-as-code-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

# Jenkins Configuration as Code Plugin

<img src="plugin/src/main/webapp/img/logo-head.svg" width="250">

Slides for Jenkins World 2018 presentation [available here](https://docs.google.com/presentation/d/1VsvDuffinmxOjg0a7irhgJSRWpCzLg_Yskf7Fw7FpBg/edit?usp=sharing).

## Release information

Release notes of the latest release can be found here: https://wiki.jenkins.io/display/JENKINS/configuration+as+code+plugin

## Jenkins Configuration as Code office hours

Every second Wednesday we meet at JcasC office hours meeting, where we discuss:
* current issues
* hot topics
* plans for the future

You can join via Hangout on Air link shared 10 minutes before the meeting on our [Gitter](https://gitter.im/jenkinsci/configuration-as-code-plugin) channel.
If no link is shared we'll use the link from the [invitation](https://calendar.google.com/event?action=TEMPLATE&tmeid=MmdwdTE1cTFvaGw1NGUycGxqdWUwcXExaWFfMjAxODA3MjVUMDcwMDAwWiBld2VAcHJhcW1hLm5ldA&tmsrc=ewe%40praqma.net&scp=ALL).

Minutes of meeting are available [here](https://docs.google.com/document/d/1Hm07Q1egWL6VVAqNgu27bcMnqNZhYJmXKRvknVw4Y84/edit?usp=sharing).

## Introduction

Setting up Jenkins is a complex process, as both Jenkins and its plugins require some tuning and configuration,
with dozens of parameters to set within the web UI `manage` section.

Experienced Jenkins users rely on groovy init scripts to customize jenkins and enforce desired state. Those
scripts directly invoke Jenkins API and as such can do everything (at your own risk). But they also require
you know Jenkins internals, and are confident in writing groovy scripts on top of Jenkins API.

Configuration-as-Code plugin has been designed as an _**opinionated**_ way to configure jenkins based on
human-readable declarative configuration files. Writing such a file should be feasible without being a Jenkins
expert, just translating into _code_ a configuration process one is used to executing in the web UI.

So, we are trying to replace this :

![configuration form](images/sample_form.png)

with this :

```yaml
jenkins:
  securityRealm:
    ldap:
      configurations:
        - server: ldap.acme.com
          rootDN: dc=acme,dc=fr
          managerPasswordSecret: ${LDAP_PASSWORD}
      cache:
        size: 100
        ttl: 10
      userIdStrategy: CaseSensitive
      groupIdStrategy: CaseSensitive
```

In addition, we want such a file to have a well documented syntax, and tooling to assist in writing and testing,
so end-users have full guidance in using this toolset and don't have to search stackoverflow for samples.

## Getting started

To get started you must have a running Jenkins instance with the plugin installed.
- For those running Jenkins as a [docker container](https://github.com/jenkinsci/docker) (and maybe also [pre-installing plugins](https://github.com/jenkinsci/docker#preinstalling-plugins)), the name of the plugin is `configuration-as-code`.

The only other requirement is that your Jenkins instance has an environment variable set that points to the current configuration location for configuration as code. So the plugin requires the environment variable `CASC_JENKINS_CONFIG` to be set. The variable can point to the following:

- Path to a folder containing a set of config files I.E `/var/jenkins_home/casc_configs`
- A full path to a single file I.E `/var/jenkins_home/casc_configs/jenkins.yaml`
- A URL pointing to a file served on the web I.E `https://mysite.com/jenkins.yaml`

If everything was setup correctly you should now be able to browse the Configuration as Code management link in `Manage Jenkins -> Configuration as Code`.

## How to configure your first instance

Generally when you are about to configure your first instance you want to first browse the examples shown in the [demos](demos)
section of this repository. If you've got a plugin, that we do not have an example for, you can consult the reference
help document by pressing the `Documentation` link on the Configuration as Code management link page. What you'll get a
web page like this:

![refence page](images/reference.png)

If you've got a specific plugin you might wish to configure, search the page for the name of the plugin. The page will
show you which root element the configuration belongs to. Most plugins you install belong in the `unclassifed` root
element.

![unclassified](images/unclassified.png)

## Examples

This configuration file includes root entries for various components of your jenkins master installation. the `jenkins`
one is for the root jenkins object, and other ones are for various global configuration elements.

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
            scope:    SYSTEM
            id:       ssh_private_key
            keyStoreSource:
              fileOnMaster:
                keyStoreFile: /docker/secret/id_rsa
```

Also see [demos](demos) folder with various samples.

## Full documentation

The configuration file format depends on the version of jenkins-core and installed plugins.
Documentation is generated from a live instance, as well as a JSON-schema you can use to validate configuration file
with your favourite yaml tools.

## How to provide initial secrets for Configuration-as-Code

Currently you can provide initial secrets to Configuration-as-Code that all rely on <key,value>
substitution of strings in configuration. Just like in Jenkins: `${some_var}`. Default variable substitution
using the `:-` operator from `bash` is also available:
`key: ${VALUE:-defaultvalue}` will evaluate to `defaultvalue` if `$VALUE` is unset. We can provide these initial secrets in the following ways:

 - Using environment variables
 - Using docker-secrets, where files on path `/run/secrets/${KEY}` will be replaced by `${KEY}` in configuration
 - Using kubernetes secrets, logic is the same as for docker-secrets (see example in [demos](demos) folder)
 - Using vault, see instructions in section below

### Using Vault initial secrets

**Prerequisites**

 - The environment variable `CASC_VAULT_PW` must be present, if token is not used (Vault password)
 - The environment variable `CASC_VAULT_USER` must be present, if token is not used (Vault username)
 - The environment variable `CASC_VAULT_TOKEN` must be present, if U/P is not used (Vault token)
 - The environment variable `CASC_VAULT_PATH` must be present (Vault key path, I.E /secrets/jenkins)
 - The environment variable `CASC_VAULT_URL` must be present (Vault url, including port)
 - The environment variable `CASC_VAULT_MOUNT` is optional (Vault auth mount, I.E `ldap` or another username & password authentication type, defaults to `userpass`)
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
version: '3.6'

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

**TODO** provide a dockerfile to 'build' this documentation from specified jenkins-core release and plugins.

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

## Supported plugins

Most plugins should be supported out-of-the-box, or maybe require some minimal changes.
You can check [this dashboard](https://issues.jenkins-ci.org/secure/Dashboard.jspa?selectPageId=17346) for known compatibility issues.

## Jenkins Enhancement Proposal

As Configuration-as-code is demonstrated to be a highly requested topic in Jenkins community, we have published
[JEP 201](https://github.com/jenkinsci/jep/tree/master/jep/201) as proposal to make this a standard component
of the Jenkins project.

Current status : accepted.
