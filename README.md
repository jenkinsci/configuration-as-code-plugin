# Jenkins Configuration-as-Code Plugin
This plugin helps configuration management tools like Chef/Puppet to deploy Jenkins
by allowing you to describe Jenkins configuration through human-writable configuration files.

You place these configuration files as `$JENKINS_HOME/conf/*.json`, or alternatively
you specify a directory that contains `*.json` through the `JENKINS_CONF` environment variable or jenkinsConf
system property. If you have both env variable and system property set then plugin will read system property first,
then read env variable (override system property). If nothing is set plugin defaults to `$JENKINS_HOME/conf/`

Take a look on example configuation files under src/main/resources/examples

# Implementing support for the plugin

* Add dependency 
When it is all done, and we have a name and release automation for the plugin - Add <final plugin name> as dependency to your project <Maven coordinates>
For now - you can add your implementation to this plugin source code base to test it

* In order to implement support for your plugin you need to do two things - define JSON schema to describe confuguration file and extend abstract Configurator class

### JSON schema.
Used to validate configuration file before we process it. Also, you can use [jsonschema2pojo](http://www.jsonschema2pojo.org/) to generate classes from schema and then use them to deserialise configuration files.
Make sure that you implement org.jenkinsci.plugins.systemconfigdsl.api.ConfigurationDescription. Could be achived by adding the follwing line to your schema
```
"javaInterfaces" : ["org.jenkinsci.plugins.systemconfigdsl.api.ConfigurationDescription"],
```
Example:

```
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "javaType": "org.jenkinsci.plugins.systemconfigdsl.impl.credentials.generated.UsernamePasswordCrendetialsConfig",
  "javaInterfaces" : ["org.jenkinsci.plugins.systemconfigdsl.api.ConfigurationDescription"],
  "properties": {
    "credentials": {
      "type": "array",
      "items": {
        "type": "object",
        "javaType": "org.jenkinsci.plugins.systemconfigdsl.impl.credentials.generated.UsernamePasswordCrendetials",
        "properties": {
          "scope": {
            "type": "string",
            "default": "global"
          },
          "userId": {
            "type": "string"
          },
          "credentialsId": {
            "type": "string"
          },
          "description": {
            "type": "string",
            "default": ""
          },
          "path": {
            "type": "string"
          }
        }
      },
      "additionalProperties": false,
      "required": [
        "userId",
        "credentialsId",
        "path"
      ]
    }
  },
  "additionalProperties": false,
  "required": [
    "credentials"
  ]
}
```

Example configuration file that corresponds to schema above

```
{
  "credentials": [
    {
      "scope": "global",
      "userId": "publisher",
      "credentialsId": "artifactory-publisher",
      "description": "",
      "path": "/Users/andrey9kin/code/system-config-dsl-plugin/work/conf/password"
    }
  ]
}
```

### Extending abstract Configurtor class.
When extending org.jenkinsci.plugins.systemconfigdsl.api.Configurator annotate your class wiht `@AutoService` annotation. We are relying on [ServiceLoader](https://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html) to discover all implementations supplied by the plugins. `@AutoService` annotation provided by the following dependency.

```
    <dependency>
      <groupId>com.google.auto.service</groupId>
      <artifactId>auto-service</artifactId>
      <version>1.0-rc3</version>
    </dependency>
```

We are using naming convention to match configurators and configurations. Name of the json file containing configuration and schema file should be the same as the string returned by the getConfigFileSectionName method. Recomendation is to use your plugin short name (plugin id)

When implementing configure method consider the following:
* There are two primary scenarios for configuration. One is phoenix server when configuration applied only once on startup. When configuration needed then installation is recreated from scratch. Think deploying in Docker containers. The second scenario is when you do hot reload of configation, i.e. reconfiguring already existing server which is already configured. In the first case you just apply configuration. In the second you have to check if something already exists and then reconfigure existing objects. For instance, it is a bad idea to just create one more Artifactory server object and add it to the list of Artifactory servers since it will be one there already. And it could be so that it will have the same configuration.
* Consider implementing possibility to do a dry run. In this case we do not apply configuration but only print what will happen if we would apply it
* Since Configurator implementation is distributed together with the plugins we don't need to support multiple versions of the plugin at the same time. However it is still a good idea to keep backward compatibility for schema and configuration files, i.e. if some field isn't used anylonger then do not remove it from schema straight away - print warning message that it is depricated and will be soon removed. In this way you will give a chance to people to adjust their configuration without breaking things. Also, update migration guide in your plugin documentation. Do not have one? Time to add it.

# See also

See [JENKINS-31094](https://issues.jenkins-ci.org/browse/JENKINS-31094) as the context.
Also see [Jenkins Job DSL plugin](https://github.com/jenkinsci/job-dsl-plugin)

# Advanced Features

* [Scripting capabilities](docs/scripting.md) help you define repetitive things concisely
* [Installing plugins](docs/plugin.md)
