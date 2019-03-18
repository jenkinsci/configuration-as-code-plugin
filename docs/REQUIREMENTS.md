# JCasC Requirements - guide for plugin maintainers

JCasC is designed so any plugin can be managed without the need to implement any custom
API, but still require plugins to respect some contract, aka "_convention over extension_".
This documentation is here to explain plugin maintainers those conventions and provide guidance
on expected design.

![JCasC is coming](BraceYourselves.jpg)

## Overview

JCasC relies on the ability to introspect Jenkins configurable components to build a "data model"
from a live Jenkins instance. For this purpose it relies on web UI data-binding conventions.

For legacy reasons, Jenkins offers multiple ways to support UI data-binding, but the sole
one to be introspection friendly is to offer `@DataBoundSetter` fields or setters in your code.

Surprisingly, this is well adopted by most plugins for `Describable` components, but not for
`Descriptor`s, despite the exact same mechanism can be used for both. And unfortunately, in
many cases the interesting components to offer configuration one want to expose to JCasC
is attached to a Descriptors.

## Check List

### Rule 1: don't write code for data-binding

Check implementation of `Descriptor#configure(StaplerRequest,JSONObject)` in your descriptors.
This one should **not** use any of the `JSONObject.get*()` methods to set value for an internal
field. Prefer exposing JavaBean setter methods, and use `req.bindJSON(this,JSONObject)` to rely
on introspection-friendly data-binding.

Within a Descriptor such setters don't have to be annotated as `@DataBoundSetter` but we suggest
to do anyway, as it makes their intent more clear.

sample:

```java
public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
    smtpHost = nullify(json.getString("smtpHost"));
    replyToAddress = json.getString("replyToAddress");
    ...
    save();
    return true;
}
```

to be replaced by:

```java
public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
    try (BulkChange bc = new BulkChange(this)) {
        req.bindJSON(this, json);
        bc.commit();
    }
    return true;
}

@DataBoundSetter
public void setSmtpHost(String smtpHost) {
    this.smtpHost = nullify(smtpHost);
    save();
}

@DataBoundSetter
public void setReplyToAddress(String address) {
    this.replyToAddress = Util.fixEmpty(address);
    save();
}
```

Notes:
- You also need matching getters for jelly view to render current value, but you probably already have them declared.
- Use of `BulkChange` allows avoiding repeated calls to `save()` to actually persist to disk only once fully
configured.
- You might not even need to implement `configure` once [#3669](https://github.com/jenkinsci/jenkins/pull/3669)
is merged.

### Rule 2: don't use pseudo-properties for optional

You might have a set of fields which only make sense when set altogether, and have jelly view
to use `<f:optionalBlock>` based on some boolean pseudo-property to show/hide the matching section
in web UI.

Doing so requires had-written data-binding code, so based on rule 1 should be prohibited.

Hopefully there's a simple (and arguably better) way to handle this, by just using nested components
and group all related fields into an optional sub-element.

sample:

```xml
<f:optionalBlock name="useAuth" title="${%Use Authentication}"
                 checked="${descriptor.username!=null}">
    <f:entry title="${%User Name}" field="username">	
          <f:textbox />	
    </f:entry>
    ...
```

```java
private String username;
private Secret password;

public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
    if(json.has("useAuth")) {
        JSONObject auth = json.getJSONObject("useAuth");
        username = nullify(auth.getString("username"));
        password = Secret.fromString(nullify(auth.getString("password")));	
    }
}
```

to be replaced by:

```xml
<f:optionalProperty field="Authentication" title="${%Use Authentication}"/>
```

```java
private Authentication authentication;
```

With a fresh new `Authentication` Describable class to host `username` and `password`, all
the `<f:optionalBlock>` body being moved `Authentication/config.jelly` view.

Note: This also requires some data migration logic, please read [PLUGINS](PLUGINS.md) for a step
by step migration guide.

### Rule 3: define a test case if you can

Checking support for JCasC is easy as long as your plugin requires Java 8 / Jenkins 2.60+.

You just need the Configuration as Code plugin as a test dependency and a sample YAML file for your component

```xml
<dependency>
    <groupId>io.jenkins</groupId>
    <artifactId>configuration-as-code</artifactId>
    <version>1.5</version>
    <scope>test</scope>
</dependency>
```

```java
public class ConfigAsCodeTest {

    @Rule public JenkinsRule r = new JenkinsRule();

    @Test public void should_support_configuration_as_code() throws Exception {
        ConfigurationAsCode.get().configure(ConfigAsCodeTest.class.getResource("configuration-as-code.yml").toString());
        assertTrue( /* check plugin has been configured as expected */ );
    }
```

Benefits for you to write such a test case:

- You confirm your plugin is well designed regarding JCasC conventions
- You offer users a sample configuration file
- You will be able to detect breaking changes that may impact your users

### Rule 4: ping us in case of doubts

Really, if you need any assistance getting your plugin to support JCasC, want code review
or anything, ping us on [Gitter](https://gitter.im/jenkinsci/configuration-as-code-plugin).
