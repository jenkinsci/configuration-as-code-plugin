# Developer documentation

This document describes the JCasC API and design for plugin developers who are interested in
extending JCasC by implementing custom Configurators or re-using the configuration mechanism
in another context.

## Using Configuration as Code

### Configurators

Third party code to rely on JCasC has to create a key:value hierarchical representation of the target
component and its sub-components. This representation is defined by the `io.jenkins.plugins.casc.model` package,
and does not force use on any specific file format, despite we focus on YAML.

The main API is the `Configurator` which encapsulates access to the target component data model and how to create/configure it.
Such a data model is exposed to external usage as a set of `Attribute`s via the `Configurator.describe()` method.
Each key in the key:value representation used as configuration input, has to match an `Attribute`.

### ConfigurationContext

The configuration process only relies on `ConfigurationContext` to convert the key:value representation into a live
component instance. Third party components to use this mechanism can provide a custom context, while
JCasC relies on registered Jenkins components.

`ConfigurationContext` provides:

- tweaks support for deprecated and restricted attributes, as well as unknown input elements
- defines registry to retrieve Configurator for various component and classes to be configured
- offers option to register `Listener`s to get notified about the configuration process and react on errors

### YAML support

`io.jenkins.plugins.casc.yaml` package defines the implementation for loading the configuration from YAML sources.
`YamlUtils.loadFrom` encapsulates the YAML parsing and merge process from a set of YAML documents, while `YamlSource`
abstracts the way we load documents from files, URLs, or any other sources.

## Extending Configuration as Code

`Configurator` and `Attribute` are the core abstraction of Configuration as Code to offer implementation flexibility.
JCasC offers an implementation based on introspecting Java classes, relying on web UI data-binding
mechanisms for `DataBound` component, and on JavaBean conventions for others (Descriptors, Extensions).

To implement Configurator for some component which doesn't fit into this model, or to control the exposed data
model without relying on introspection, one can extend `BaseConfigurator` and override the `describe()` method to
control the exposed data model.

`Attribute`s exposed by this data model only have to define how to set value on target component and how to retrieve
current value from a live instance (used by `export` feature). Here again JCasC offers as default
implementation a JavaBean compliant implementation, but one could override get and/or set operation with custom
code to support alternate mechanisms.
