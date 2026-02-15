# How to migrate your traditionally configured Jenkins

So you're tired of manually configuring Jenkins each time you want to introduce a plugin or change existing setup?
Or maybe you're not feeling confident enough with the change you have in mind?

Jenkins Configuration as Code solves both problems - you don't need to access Jenkins to implement a change and you can always revert to previous version of configuration (if you keep your configuration under version control)

## Create jenkins.yaml from scratch

We've decided to use the YAML format so writing the configuration "by hand" should be easy. Your existing Jenkins can be also used as a documentation - the YAML file tries to mimic the UI you're used to as much as possible.

Plugin provides documentation generated for your specific Jenkins instance - after you install it, and it is available at:
`http://[your_jenkins_url]/configuration-as-code/`

Various samples of plugins' configuration can be found in [demos](../demos) folder

## Export existing configuration

To be able to do that, you need to install the plugin manually on your working Jenkins instance and use the [export function](/docs/features/configExport.md). 
This step will produce an initial configuration you can use to create configuration YAMLs for your Jenkins instance.
