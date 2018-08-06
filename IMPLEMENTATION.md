# Jenkins Configuration as Code : implementation details

Input configuration file uses a YAML hierarchical data structure.
every node of this data structure is passed to a `Configurator` responsible
to apply the adequate configuration on Jenkins live instance.

## Configurator

A [`Configurator`](https://github.com/jenkinsci/configuration-as-code-plugin/blob/master/plugin/src/main/java/io/jenkins/plugins/casc/Configurator.java)
is managing a specific Jenkins Component, and as such knows
about data this component exposes to end-user for configuration.
It has:

* a `name` to match a YAML entry,
* a `target` component type
* a `describe` method to document the attributes the target component exposes to configuration
* a `configure` method to configure the target component

From a yaml node with associated `Configurator`, configuration-as-code will handle every
child node in YAML structure based on current node's `Attribute`s, as described by the `Configurator`.

## Root Configurator selection

Root elements are identified by YAML entry name, and a matching
[`RootElementConfigurator`](https://github.com/jenkinsci/configuration-as-code-plugin/blob/master/plugin/src/main/java/io.jenkins/plugins/casc/RootElementConfigurator.java) is selected.

`RootElementConfigurator` is a special interface used to identify a `Configurator` which manages a top level
configuration element in the yaml document.

configuration-as-code do provide a custom `RootElementConfigurator` for `jenkins` root entry in yaml document,
as well as generic `RootElementConfigurator`s to model [global configuration categories](https://github.com/jenkinsci/jenkins/blob/master/core/src/main/java/jenkins/model/GlobalConfigurationCategory.java).

### Attributes

Configurator do document the target component by implementing `describe` method. This one do returl a set
of [`Attribute`](https://github.com/jenkinsci/configuration-as-code-plugin/blob/master/plugin/src/main/java/io.jenkins/plugins/casc/Attribute.java)s
to document both name _AND_ type for a configurable attribute.

We don't want to expose the whole Jenkins Java API to configuration-as-code. Many components do define setter
methods for technical reasons or to support backward compatibility. So configuration-as-code do exclude :
* setter methods marked as `@Deprecated`
* setter methods marked as `@Restricted`

Attribute also is responsible to document the target component type. We use Java reflexion to extract this
information from Java API, including parameterized types.

As a resume, a component which exposes a method like :
```java
public void setFoo(List<Foo`> foos) { ... }

```
will be detected as Attribute named `foo` with target type `Foo` with multiple values expected.

### Configuration

Configurator also has to implement the `configure` method to process yaml content and instantiate or configure
target component. The actual mechanism depends on the target. Configuration-as-Code do provide generic
mechanisms which cover most Jenkins components.

This generic mechanism assumes Jenkins core components and plugins do follow some conventions, but
custom "glue-code" can also be provided as a (temporary) workaround, or to expose a configuration model
which doesn't reflect the internal data model, but better matches the end-user experience.

A typical sample for this scenario is credentials-plugin.
[CredentialsRootConfigurator](https://github.com/jenkinsci/configuration-as-code-plugin/blob/master/plugin/src/main/java/io.jenkins/plugins/casc/credentials/CredentialsRootConfigurator.java)
do expose a simplified configuration API for `system` credentials store, which is hardly configurable
using the other general purpose configuration-as-code component due to internal design of this plugin.

## General purpose configurators

### Using Data binding

As we want to reflect web UI user experience, relying on the same configuration mechanisms as the web
UI is a natural approach.

`org.jenkinsci.plugins.casc.impl.configurators.DataBoundConfigurator` can configure arbitrary
jenkins component to rely on `DataBoundConstructor`
and `DataBoundSetter`s for UI data-binding. It uses same attributes names as
the web UI, which are expected to be human friendly.

When, for some technical or legacy reasons, technical attribute name isn't user friendly, we also support
`@Symbol` annotation on setter to offer a better user-experience.

### Descriptors

`org.jenkinsci.plugins.casc.DescriptorRootElementConfigurator` can configure
global configuration for Descriptors, to mimic the `global.jelly` UI exposed
to end user on the web UI.

Jenkins has hundreds Descriptors, most of them for internal technical reasons,
so only the ones to have a `global` view are accessible from configuration-as-code.

For Descriptors to work well with configuration-as-code, they need to follow
[some design best practices](PLUGINS.md) in terms of data binding. This is not such a common thing,
so we expect this will require some evangelism on plugin developers.

As a short terms workaround, a custom `Configurator` glue-code implementation can be implemented.

## Initial secrets

Initial secrets are handled by the concrete implementations of the [SecretSource](https://github.com/jenkinsci/configuration-as-code-plugin/blob/master/plugin/src/main/java/io.jenkins/plugins/casc/SecretSource.java). In order to implement a new
secret source, subclass `SecretSource` by extending it, and mark the new class with the `@Extension` annotation.
