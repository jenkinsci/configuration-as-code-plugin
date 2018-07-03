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

### Documentation

### Plugin self-configuration

## Tips & Tricks

## How to report issues