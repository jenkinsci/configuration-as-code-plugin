[![Build Status](https://ci.jenkins.io/job/Plugins/job/configuration-as-code-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/configuration-as-code-plugin/job/master/)
[![Gitter](https://badges.gitter.im/jenkinsci/configuration-as-code-plugin.svg)](https://gitter.im/jenkinsci/configuration-as-code-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

# Jenkins Configuration as Code Plugin

<img src="plugin/src/main/webapp/img/logo-head.svg" width="250">

## Release information

Release notes of the latest release can be found here: https://wiki.jenkins.io/display/JENKINS/configuration+as+code+plugin

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

Have a look at [this presentation](https://docs.google.com/presentation/d/1-irLGTAvMe8Fz1md1zkJtpIpYo9NItZKnsgTYnIqbZU/edit?usp=sharing) for more details.

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
    slave:
      name: "static-slave"
      remoteFS: "/home/jenkins"
      launcher: "jnlp"

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
    smtpHost: smtp.acme.org
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

## How to create initial "seed" job

Configuration is not just about setting up jenkins master, it's also about creating an initial set of jobs.
For this purpose, we delegate to the popular [job-dsl-plugin](https://wiki.jenkins.io/display/JENKINS/Job+DSL+Plugin)
and run a job-dsl script to create an initial set of jobs.

Typical usage is to rely on a multi-branch, or organization folder job type, so further jobs will be dynamically
created. So a multi-branch seed job will prepare a master to be fully configured for CI/CD targeting a repository
or organization.

Job-DSL plugin uses groovy syntax for it's job configuration DSL, so you'll have to mix yaml and groovy within your
configuration-as-code file:

```yaml
jenkins:
  systemMessage: "Simple seed job example"
jobs:
  - >
      multibranchPipelineJob('configuration-as-code') {
          branchSources {
              git {
                  remote('https://github.com/jenkinsci/configuration-as-code-plugin.git')
              }
          }
      }
```


## How to provide initial secrets for Configuration-as-Code

Currently you can provide initial secrets to Configuration-as-Code that all rely on <key,value>
substitution of strings in configuration. Just like in Jenkins: `${some_var}`. We can provide these initial secrets in
the following ways:

 - Using environment variables
 - Using docker-secrets, where files on path `/run/secrets/${KEY}` will be replaced by `${KEY}` in configuration
 - Using vault, see instructions in section below

### Using Vault initial secrets

**Prerequisites**

 - The environment variable `CASC_VAULT_PW` must be present (Vault password)
 - The environment variable `CASC_VAULT_USER` must be present (Vault username)
 - The environment variable `CASC_VAULT_PATH` must be present (Vault key path, I.E /secrets/jenkins)
 - The environment variable `CASC_VAULT_URL` must be present (Vault url, including port)
 - The environment variable `CASC_VAULT_MOUNT` is optional (Vault auth mount, I.E `ldap` or another username & password authentication type, defaults to `userpass`)

If all those 4 are present, Configuration-as-Code will try to gather initial secrets from Vault. Requires read access for the configured user.

**TODO** provide a dockerfile to 'build' this documentation from specified jenkins-core release and plugins.

## Supported plugins

Here is a list of plugin we have successfully tested to support configuration-as-code approach :

 - [x] active directory plugin ([details](demos/active-directory/README.md))
 - [x] artifactory plugin ([details](demos/artifactory/README.md))
 - [x] credentials plugin ([details](demos/credentials/README.md))
 - [x] docker plugin ([details](demos/docker/README.md))
 - [x] git plugin ([details](demos/git/README.md))
 - [x] ldap plugin ([details](demos/ldap/README.md))
 - [x] mailer plugin with some limitations ([details](demos/mailer/README.md))
 - [x] tfs plugin with some limitations ([details](demos/tfs/README.md))
 - [x] workflow-cps-global-lib _aka_ "global libraries" ([details](demos/workflow-cps-global-lib/README.md))
 - [x] matrix-auth-plugin ([details](demos/global-matrix-auth/README.md))
 - [ ] role-strategy-plugin ([details](demos/role-strategy-auth/README.md))
 - [x] warnings-plugin (>= 4.66) ([details](demos/warnings/README.md))
 - [x] kubernetes plugin ([details](demos/kubernetes/README.md))
 - [ ] more to come soon...

## Jenkins Enhancement Proposal

As Configuration-as-code is demonstrated to be a highly requested topic in Jenkins community, we have published
[JEP 201](https://github.com/jenkinsci/jep/tree/master/jep/201) as proposal to make this a standard component
of the Jenkins project.

Current status : draft.
