# Getting started

## Installation

Until first official release is available you have to use Experimental Update Center to try out the plugin.

### Configuring Jenkins to Use Experimental Update Center
Users who are interested in downloading experimental plugin releases can go to Plugin Manager, then to the Advanced tab, and configure the update center URL `https://updates.jenkins.io/experimental/update-center.json`. Save, and then select Check Now. Experimental plugin updates will be marked as such on the Available and Updates tabs of the Plugin Manager.

Once you install alpha version of Configuration as Code plugin, you can switch back to the default `https://updates.jenkins.io/update-center.json` update center URL.

(from [jenkins.io](https://jenkins.io/doc/developer/publishing/releasing-experimental-updates/))

## Configuration
Once the plugin is installed you can use yaml file to configure your Jenkins instance. For the plugin to know where to look for such configuration file please set up `CASC_JENKINS_CONFIG` to point to a folder with a set of yaml files, or to a file itself - if you keep your configuration in one file.

`CASC_JENKINS_CONFIG` can be configured as a location on disk or direct url to the yaml file.

If you don't configure the variable it will look for `jenkins.yaml` in $JENKINS_HOME

### Documentation

Documentation is automatically generated, based on your Jenkins version and installed plugins. It is available under `Manage Jenkins` -> `Configuration as Code` -> `Documentation`
 
## JCasC self-configuration

Plugins allows you to configure its behaviour regarding the usage of deprecated and restricted attributes. By default you can configure plugins' using deprecated/restricted methods/attributes but you can alter the behaviour, e.g.:

```
configuration-as-code:
  version: 1
  deprecation: warn
  restricted: warn
```

## Plugins installation

Use `plugins` root element in your yaml to provide a list of required plugins
If you're using docker you can still use existing solution with `install-plugins.sh` script

## Tips & Tricks

If the plugin you want to configure is installed but you can't see it in generated documentation or you can see configurator, but it has no attributes it most probably means the plugin is not compatible with JcasC. 

Have a look at [PLUGINS.md](PLUGINS.md) to find more information about what is required from a plugin to be compatible with JcasC, and to fix it if needed.

## How to report issues

Github issues are used to track plugin development, also bugs can be reported that way.

If you prefer [issues.jenkins-ci.org](issues.jenkins-ci.org) please use _configuration-as-code-plugin_ component.