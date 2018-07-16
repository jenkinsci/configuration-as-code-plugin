# How to migrate your traditionally configured Jenkins

So you're tired of manually configuring Jenkins each time you want to introduce a plugin or change existing setup?
Or maybe you're not feeling confident enough with the change you have in mind?

Jenkins Configuration as Code solves both problems - you don't need to access Jenkins to implement a change and you can always revert to previous version of configuration (if you keep your configuration under version control)

## Create jenkins.yaml from scratch

We've decided to use yaml format so writing the configuration "by hand" should be easy. Your existing Jenkins can be also used as a documentation - yaml file tries to mimic UI you're used to as much as possible.

Plugin provides documentation generated for your specific Jenkins instance - after you install it, and it is available at:
http://[your_jenkins_url]/configuration-as-code/

Various samples of plugins' configuration can be found in [demos](../demos) folder

## Export existing configuration

To be able to do that you need to install the plugin manually on your working Jenkins instance and use export function under http://[your_jenkins_url]/configuration-as-code/. Please note that, at this time, the produced yaml output might not be usable as-is
as input for Configuration-as-Code. A strict roundtrip between export and configure is under consideration but might require some 
changes in jenkins-core and few plugins.
