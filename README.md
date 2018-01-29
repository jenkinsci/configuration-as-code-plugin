[![Build Status](https://ci.jenkins.io/job/Plugins/job/configuration-as-code-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/configuration-as-code-plugin/job/master/)

# Jenkins Configuration as Code Plugin

![logo](logo.png)

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

![configuration form](sample_form.png)

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

## Jenkins Enhancement Proposal 

As Configuration-as-code is demonstrated to be a highly requested topic in Jenkins community, we have published
[JEP 201](https://github.com/jenkinsci/jep/tree/master/jep/201) as proposal to make this a standard component
of the Jenkins project.

Current status : proposal accepted.

## Releases

There's no release yet. 

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

mailer:
  adminAddress: admin@acme.org
  replyToAddress: do-not-reply@acme.org
  smtpHost: smtp.acme.org
  smtpPort: 4441
  
credentials:
  system:
    ? # "global"
    : - certificate:
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
 
If all those 4 are present, Configuration-as-Code will try to gather initial secrets from Vault. Requires read access for the configured user.

**TODO** provide a dockerfile to 'build' this documentation from specified jenkins-core release and plugins.


## Supported plugins

Here is a list of plugin we have successfuly tested to support configuration-as-code approach :

 - [x] active directory plugin ([details](demos/credentials/README.md))
 - [x] artifactory plugin ([details](demos/artifactory/README.md))
 - [x] credentials plugin ([details](demos/credentials/README.md))
 - [x] docker plugin ([details](demos/docker/README.md))
 - [x] git plugin ([details](demos/git/README.md))
 - [x] ldap plugin ([details](demos/ldap/README.md))
 - [x] mailer plugin with some limitations ([details](demos/mailer/README.md))
 - [x] tfs plugin with some limitations ([details](demos/tfs/README.md))
 - [x] workflow-cps-global-lib _aka_ "global libraries" ([details](demos/workflow-cps-global-lib/README.md))                  
 - [ ] more to come soon... 
