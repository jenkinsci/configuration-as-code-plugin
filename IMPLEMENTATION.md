# Jenkins Configuration as Code : implementation details

Input configuration file uses a YAML hierarchical data structure.
every node of this data structure is passed to a `Configurator` responsible
to apply the adequate configuration on Jenkins live instance.

## Configurator

A `Configurator` is managing a specific Jenkins Component, and as such knows
about data this component exposes to end-user for configuration. 
It has:
 
* a `name` to match a YAML entry,
* a `target` component type (in most cases extension point implemented by component)
* a `describe` method to document the attributes the target component exposes to configuraiton
* a `configure` metod to configure the target component  

## Configurator selection

Root elements are identified by YAML entry name, and a matching 
`RootElementConfigurator` is selected.

Child elements are identified by YAML entry name _AND_ expected attribute
type, so in many case selecting the right `Configurator` is based on 
finding the matching implementation for a known set of candidates.
 
## General purpose configurators

`org.jenkinsci.plugins.casc.DescribableConfigurator` can configure arbitrary 
jenkins component to implement `Describable` and rely on `DataBoundConstructor`
and `DataBoundSetter`s for UI data-binding. It uses same attributes names as
the web UI, which are expected to be human friendly. 

`org.jenkinsci.plugins.casc.DescriptorRootElementConfigurator` can configure
global configuration for Descriptors, to mimic the `global.jelly` UI exposed
to end user on the web UI. 

Jenkins has hundreds Descriptors, most of them for internal technical reasons,
so only the ones to have a `global.jelly` view are accessible from 
configuration-as-code.
For Descriptors to work well with configuration-as-code, they need to follow
[some design best practices](PLUGINS.md)

 
