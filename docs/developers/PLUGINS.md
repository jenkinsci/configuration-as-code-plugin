# Guide to plugin developers

Configuration as Code relies on `Describable` and `DataBound` mechanism Jenkins plugin developers are probably already using.
As long as you follow [best practices](https://jenkins.io/doc/developer/plugin-development/pipeline-integration/#constructor-vs-setters)
using those annotations for data-binding, same attributes will be usable for configuration-as-code.

## Descriptors global configuration

Most of the interesting plugin's configuration you want to expose to end users with JCasC is managed by your plugin's
`Descriptor`(s) and exposed on web UI with a `global.jelly` view. This is fully supported by JCasC as long as you rely on
the exact same `DataBound` mechanism, which isn't a common practice (yet).

In many plugins, `Descriptor#configure()` is implemented by lookup for attributes values from the `JSONObject`. To make your `Descriptor`
compliant with JCasC, you'll need to expose your configuration attributes as `@DataBoundSetters`.

Before you start, make sure the following pre-conditions are met:

- You are using a recent version of plugin [parent pom](https://github.com/jenkinsci/plugin-pom/releases).

```xml
<parent>
    <groupId>org.jenkins-ci.plugins</groupId>
    <artifactId>plugin</artifactId>
    <version>THE_PARENT_POM_VERSION_HERE</version>
    <relativePath />
</parent>
```

- The Jenkins core version of your plugin are aligned with the Configuration as Code plugin versions (also in the [pom.xml](/pom.xml)).

```xml
<properties>
    <jenkins.version>THE_JENKINS_CORE_VERSION_HERE</jenkins.version>
</properties>
```

Here's the recommended approach:

Lets consider this `Descriptor`:

```java
public static final class DescriptorImpl extends Descriptor<Foo> {
   private String charset;

   /** optional password */
   private Secret password;

   public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
       charset = json.getString("charset");
       if (json.has("usePassword")) {
           password = Secret.fromString(nullify(auth.getString("password")));
       } else {
           password = null;
       }
       save();
       return true;
   }

   public String getCharset() { return charset; }
}
```

with global.jelly view:

```xml
<j:jelly xmlns:j="jelly:core" xmlns:f="/lib/form">
   <f:entry title="${%Charset}" field="charset">
        <f:textbox />
   </f:entry>

   <f:optionalBlock name="usePassword" title="${%Use Authentication}" checked="${descriptor.password!=null}">
      <f:entry title="${%Password}" field="password">
          <f:password />
      </f:entry>
   </f:optionalBlock>
</j:jelly>
```

### Step 1

Define `@DataBoundSetters` JavaBean setters for your `Descriptor`'s properties. They should match the getters you already have for
`global.jelly` data-binding.

```java
   @DataBoundSetter
   public void setCharset(String charset) {
       this.charset = charset;
   }
```

### Step 2

Create a new `Describable` object with a `config.jelly` view to own optional attributes.

```java
public class Authentication extends AbstractDescribableImpl<PAuthentication> {

    private Secret password;

    @DataBoundConstructor
    public Authentication(Secret password) { this.password = password; }

    public Secret getPassword() { return password; }

    @Extension
    public static class DescriptorImpl extends Descriptor<Authentication> {
    }
}
```

```xml
<j:jelly xmlns:j="jelly:core" xmlns:f="/lib/form">
      <f:entry title="${%Password}" field="password">
        <f:password />
      </f:entry>
</j:jelly>
```

### Step 3

Define a new attribute in your `Descriptor` to own optional attributes.
For binary compatibility you'll need to maintain the legacy getters as delegates to this new sub-component.
For backward compatibility, use `readResolve` method to create the new nested component from legacy attributes.

```java
public static final class DescriptorImpl extends Descriptor<FooBar> {
   private String charset;

   /** @deprecated use {@link #authentication} */
   private transient Secret password;

   @CheckForNull
   private Authentication authentication;

   // --- backward compatibility

   /** @deprecated use {@link #getAuthentication()} */
   public Secret getPassword() { return authentication != null ? authentication.getPassword() : null; }

   private Object readResolve() {
       if (this.password != null) {
           this.authentication = new Authentication(password);
       }
       return this;
   }
```

### Step 4

Replace `optionalBlocks` in your jelly view with `optionalProperty` and add the required `DataBound` accessors

```xml
   <f:entry title="${%Charset}" field="charset">
        <f:textbox />
   </f:entry>

   <f:optionalProperty title="${%Use Authentication}" field="authentication"/>
```

```java
   public Authentication getAuthentication() { return this.authentication; }

   @DataBoundSetter
   public void setAuthentication(Authentication authentication) { this.authentication = authentication; }
```

### Step 5

Rewrite `Descriptor#configure()` implementation to rely on `request.bindJson(this, json)`. You will have to reset attributes to their
default values as a `Descriptor` is a mutable object, i.e. data-binding won't reset values if they are not present in the JSON payload.

```java
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            // reset optional authentication to default before data-binding
            this.authentication = null;
            req.bindJSON(this, json);
            save();
            return true;
        }
```

### Step 6

If you don't have one already, define a `@Symbol` annotation on your descriptor. This is the name an end user will be able to use to access
your `Descriptor` for configuration. To avoid collisions with other plugins, prefer using your plugin's artifactId as a symbolic name for your
descriptor.

```java
@Symbol("foo")
public static final class DescriptorImpl extends Descriptor<Foo> {
```

See [mailer plugin#39](https://github.com/jenkinsci/mailer-plugin/pull/39) for a sample on required changes.

## How to test

Simplest option for you to test JCasC compatibility in your plugin is to introduce a simple test case.

[![GitHub release](https://img.shields.io/github/release/jenkinsci/configuration-as-code-plugin.svg?label=release)](https://github.com/jenkinsci/configuration-as-code-plugin/releases/latest)

Add the Configuration as Code plugin as a test dependency in your pom.xml:

```xml
<properties>
    <configuration-as-code.version>LATEST_RELEASE</configuration-as-code.version>
</properties>
<dependency>
    <groupId>io.jenkins</groupId>
    <artifactId>configuration-as-code</artifactId>
    <version>${configuration-as-code.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.jenkins.configuration-as-code</groupId>
    <artifactId>test-harness</artifactId>
    <version>${configuration-as-code.version}</version>
    <scope>test</scope>
</dependency>
```

### Configuration test

Add a new test case to load a reference configuration YAML file designed to set configurable properties of your plugin:

```java
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ConfigurationAsCodeTest {

    @Rule public JenkinsConfiguredWithCodeRule r = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    public void should_support_configuration_as_code() throws Exception {
        assertTrue( /* check plugin has been configured as expected */ );
    }
}
```

Doing so, you will confirm JCasC is able to introspect your plugin and build the expected configuration data model, but also detect
some changes made to your plugin break this configuration model.

### Backward compatibility test

About the latter, in case you need to introduce some breaking changes, you can define a backward compatibility test case:

```java
    @Test
    @ConfiguredWithCode("obsolete-configuration-as-code.yml")
    public void should_be_backward_compatible() throws Exception {
        assertTrue( /* check plugin has been configured as expected */ );
    }
```

Within this `obsolete-configuration-as-code.yml` configuration file, use the legacy data model in use before the change you introduced, and enable JCasC support for deprecated methods:

```yaml
configuration-as-code:
  deprecated: warn
```

This will let JCasC consider any `@Deprecated` setter in your component as a valid attribute to be set, enabling backward compatibility,
while the canonical JCasC model evolves to match the changes you made.

### Model export test

You also can write a test case to check export from a live instance is well supported:

```java
    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    public void should_support_configuration_export() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getUnclassifiedRoot(context).get("<your-attribute>");

        String exported = toYamlString(yourAttribute);

        String expected = toStringFromYamlFile(this, "expected_output.yaml");

        assertThat(exported, is(expected));
    }
```

The helper methods shown are provided by the `io.jenkins.plugins.casc.misc.Util` class.

_Hint:  Use a class rule if you just want to test a single import and export._

```java
public class ConfigurationAsCodeTest {

    @ClassRule
    @ConfiguredWithCode("configuration-as-code.yml")
    public static JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();
    
    @Test
    public void should_support_configuration_as_code() throws Exception {
     ...
    }
    
    @Test
    public void should_support_configuration_export() throws Exception {
     ...
    }
}
```

### JSON Schema Test (Beta)
We generate a JSON schema that users can use to validate their changes and provide IDE assistance, 
you can test that your plugin's example yaml file validates correctly by implementing the below test:
`SchemaGenerationTest` provides a abstraction layer to test out the plugins YAML file against the generated schema.

You can test if your YAML file validates against the schema.

Step 1

Create a YAML file for the configurators corresponding to the developed plugin.
For eg: `validJenkinsConfigurator.yml`
```yaml
jenkins:
    systemMessage: "Configured by Configuration as Code plugin"
    numExecutors: "Hello"

```

Step 2

Add a test for the YAML file

```java
    @Test
    public void validSchemaShouldSucceed() throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "validSchemaConfig.yml")), empty());
    }
```
