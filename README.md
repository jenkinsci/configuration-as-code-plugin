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

See [JENKINS-31094](https://issues.jenkins-ci.org/browse/JENKINS-31094) as the context.
Also see [Jenkins Job DSL plugin](https://github.com/jenkinsci/job-dsl-plugin)

## Scripting
The configuration syntax is defined as Groovy DSL, which comes in handy when you have more complex configuration.

For example, the following configuration file creates 10 build slaves:
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

The following example uses a function to simplify slave creations:
````
def createSlave(c,n) {
  c.slave {
    name n
    remoteFS "/tmp"
    if (n.contains("linux"))
      labelString 'linux'
    if (n.contains("windows"))
      labelString 'windows'
  }
}
nodes { c ->
  createSlave(c,'linux-eea3')
  createSlave(c,'linux-8d0a')
  createSlave(c,'windows-0c03')
}
````
In the above example, we use the variable `c` to refer to the context that defines various builder methods,
so that it can be passed into a function.
