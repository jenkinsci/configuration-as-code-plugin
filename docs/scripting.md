# Scripting
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
