# Developer's documentation

This document describe Configuration-as-Code API and design for plugin developer who are interested in 
extending Configuration-as-Code by implementing custom Configurators or re-using the configuration mechanism
in another context.

# Using Configuration-as-Code

## Configurators

Third party code to rely on Configuration as Code has to create a key:value hierarchical representation of the target
component and it's sub-components. This representation is defined by the `io.jenkins.plugins.casc.model` package, 
and does not force use on any specific file format, despite we focus on `yaml` for Configuration-as-Code.  

The main API is `Configurator` which encapsulate access to target component data model and how to create/configure it.
Such a data-model is exposed to external usage as a set of `Attribute`s via the `Configurator.describe()` method.
Each key in the key:value representation used as configuration input has to match an Attribute. 

## ConfigurationContext

The configuration process only relies on `ConfigurationContext` to convert key:value representation into a live 
component instance. Third party component to use this mechanism can provide a custom context, while 
Configuration-as-Code do rely on registered Jenkins components.
 
ConfigurationContext do provide :

- tweaks support for deprecated and restricted attributes, as well as unknown input elements
- define registry to retrieve Configurator for various component and classes to be configured
- offer option to register `Listener`s to get notified about the configuration process and react on errors.

## Yaml support

`io.jenkins.plugins.casc.yaml` package do define the implementation for loading configuration from Yaml sources.
`YamlUtils.loadFrom` encapsulate the yaml parsing and merge process from a set of yaml documents, while `YamlSource`
abstract the way we load documents from files, url, or any other sources.

      

# Extending Configuration-as-Code
 
`Configurator` and `Attribute` are the core abstraction of Configuration-as-Code to offer implementation flexibility.
Configuration-as-Code do offer implementation based on introspecting Java classes, relying on web UI data-binding
mechanisms for DataBound component, and on Java Bean conventions for others (Descriptors, Extensions).

To implement Configurator for some component which doesn't fit into this model, or to control the exposed data
model without relying on introspection, one can extend `BaseConfigurator` and override the `describe()` method to
control the exposed data model.

`Attribute`s exposed by this data model only have to define how to set value on target component and how to retrieve
current value from a live instance (used by `export` feature). Here again Configuration-as-Code do offer as default 
implementation a Java Bean compliant implementation, but one could override get and/or set operation with custom
code to support alternate mechanisms.

   