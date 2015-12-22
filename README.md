# Jenkins Configuration-as-Code Plugin
This plugin helps configuration management tools like Chef/Puppet to deploy Jenkins
by allowing you to describe Jenkins configuration through human-writable configuration files.

You place these configuration files as `$JENKINS_HOME/conf/*.conf`, or alternatively
you specify a directory that contains `*.conf` through the `JENKINS_CONF` environment variable.

Here is an example of a configuration file:
````
securityRealm 'pam'
authorizationStrategy('fullControlOnceLoggedIn') {
}

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

nodes {
  slave {
    name 'localhost1'
    remoteFS '/tmp/1'
    launcher 'jnlp'
    numExecutors 5
  }
  slave {
    name 'localhost2'
    remoteFS '/tmp/2'
    launcher 'jnlp'
    labelString 'windows foo'
  }
}
````

## Scripting
The configuration syntax is defined as Groovy DSL, which comes in handy when you have repetitive
configuration or more complex configuration.

````
nodes {
  (1..10).each { i ->
    slave {
      name "slave${i}"
      remoteFS "/var/jenkins/ws"
      launcher "ssh" {
        ...
      }
    }
  }
}
````