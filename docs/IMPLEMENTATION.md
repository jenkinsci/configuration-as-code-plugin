# Implementation Details

The configuration file uses a YAML hierarchical data structure.
Every node of this data structure is passed to a `Configurator` responsible
to apply the adequate configuration on a Jenkins live instance.

## Configurator

A [`Configurator`](https://github.com/jenkinsci/configuration-as-code-plugin/blob/master/plugin/src/main/java/io/jenkins/plugins/casc/Configurator.java)
is managing a specific Jenkins component, and as such knows
about data this component exposes to end users for configuration.

It has:

- a `name` to match a YAML entry
- a `target` component type
- a `describe` method to document the attributes the target component exposes to configuration
- a `configure` method to configure the target component

From a YAML node with an associated `Configurator`, JCasC will handle every
child node in the YAML structure based on the current node's `Attribute`s, as described by the `Configurator`.

## Root Configurator selection

Root elements are identified by YAML entry name, and a matching
[`RootElementConfigurator`](https://github.com/jenkinsci/configuration-as-code-plugin/blob/master/plugin/src/main/java/io/jenkins/plugins/casc/RootElementConfigurator.java) is selected.

`RootElementConfigurator` is a special interface used to identify a `Configurator` which manages a top level
configuration element in the YAML document.

JCasC provides a custom `RootElementConfigurator` for the `jenkins` root entry in the YAML document,
as well as generic `RootElementConfigurator`s to model [global configuration categories](https://github.com/jenkinsci/jenkins/blob/master/core/src/main/java/jenkins/model/GlobalConfigurationCategory.java).

### Attributes

`Configurator` documents the target component by implementing the `describe` method. This method returns a set
of [`Attribute`](https://github.com/jenkinsci/configuration-as-code-plugin/blob/master/plugin/src/main/java/io/jenkins/plugins/casc/Attribute.java)s
to document both name _AND_ type for a configurable attribute.

We don't want to expose the whole Jenkins Java API to JCasC. Many components define setter
methods for technical reasons or to support backward compatibility. So JCasC excludes:

- setter methods marked as `@Deprecated`
- setter methods marked as `@Restricted`

`Attribute` also is responsible to document the target component type. We use Java reflection to extract this
information from the Java API, including parameterized types.

As a resume, a component which exposes a method like:

```java
public void setFoo(List<Foo> foos) { ... }
```

will be detected as attribute named `foo` with target type `Foo` with multiple values expected.

### Configuration

`Configurator` also has to implement the `configure` method to process YAML content and instantiate or configure
the target component. The actual mechanism depends on the target. JCasC provides generic
mechanisms which cover most Jenkins components.

This generic mechanism assumes Jenkins core components and plugins follow some conventions, but
custom "glue-code" can also be provided as a (temporary) workaround, or to expose a configuration model
which doesn't reflect the internal data model, but better matches the end user experience.

A typical sample for this scenario is the [credentials](https://plugins.jenkins.io/credentials) plugin.
[`CredentialsRootConfigurator`](https://github.com/jenkinsci/configuration-as-code-plugin/blob/master/support/src/main/java/io/jenkins/plugins/casc/support/credentials/CredentialsRootConfigurator.java)
exposes a simplified configuration API for the `system` credentials store, which is hardly configurable
using the other general purpose JCasC component due to the internal design of this plugin.

## General purpose configurators

### Using Data binding

As we want to reflect web UI user experience, relying on the same configuration mechanisms as the web
UI is a natural approach.

`org.jenkinsci.plugins.casc.impl.configurators.DataBoundConfigurator` can configure arbitrary
Jenkins components to rely on `DataBoundConstructor`
and `DataBoundSetter`s for UI data-binding. It uses the same attribute names as
the web UI, which are expected to be human friendly.

When, for technical or legacy reasons, the technical attribute name isn't user friendly, we also support
`@Symbol` annotation on setters to offer a better user experience.

### Descriptors

`org.jenkinsci.plugins.casc.DescriptorRootElementConfigurator` can configure
global configuration for Descriptors, to mimic the `global.jelly` UI exposed
to end users on the web UI.

Jenkins has hundreds of Descriptors, most of them for internal technical reasons,
so only the ones that have a `global` view are accessible from JCasC.

For Descriptors to work well with JCasC, they need to follow
[some design best practices](PLUGINS.md) in terms of data binding. This is not such a common thing,
so we expect this will require some evangelism on plugin developers.

As short term workaround, a custom `Configurator` glue-code implementation can be implemented.

## Initial secrets

Initial secrets are handled by the concrete implementations of the [SecretSource](https://github.com/jenkinsci/configuration-as-code-plugin/blob/master/plugin/src/main/java/io.jenkins/plugins/casc/SecretSource.java). In order to implement a new
secret source, subclass `SecretSource` by extending it, and mark the new class with the `@Extension` annotation.
