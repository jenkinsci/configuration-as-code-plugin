# Jenkins Configuration-as-Code Plugin
This plugin helps configuration management tools like Chef/Puppet to deploy Jenkins
by allowing you to describe Jenkins configuration through human-writable configuration files.

You place these configuration files as `$JENKINS_HOME/conf/*.conf`, or alternatively
you specify a directory that contains `*.conf` through the `JENKINS_CONF` environment variable.

Here is an example of a configuration file:
````
plugin 'ldap'   # install some plugins
plugin 'git'

jenkins {
  # configure security
  securityRealm 'pam'
  authorizationStrategy('fullControlOnceLoggedIn') {
  }

  # set a couple of views with exact set of columns, etc
  views {
    list {
      name 'All'
      columns {
        status()
        weather()
        jobName()
        buildButton()
      }
    }
    list {
      name 'another'
      columns {
        status()
        jobName()
      }
    }
  }
  
  slaveAgentPort 9532
  
  # configure exactly two build agents
  nodes {
    slave {
      name 'foo'
      remoteFS '/tmp/1'
      launcher 'jnlp'
      numExecutors 5
    }
    slave {
      name 'bar'
      remoteFS '/tmp/2'
      launcher 'jnlp'
      labelString 'windows foo'
    }
  }
}
````

See [JENKINS-31094](https://issues.jenkins-ci.org/browse/JENKINS-31094) as the context.
Also see [Jenkins Job DSL plugin](https://github.com/jenkinsci/job-dsl-plugin)

# Advanced Features

* [Scripting capabilities](docs/scripting.md) help you define repetitive things concisely
* [Installing plugins](docs/plugin.md)
