# Guide to plugin developers

Configuration as Code relies on `Describable` and `DataBound` mechanism Jenkins plugin developers are probably already using. 
As long as you follow [best practices](https://jenkins.io/doc/developer/plugin-development/pipeline-integration/#constructor-vs-setters) 
using those annotations for data-binding, same attributes will be usable for configuration-as-code.

## Descriptors global configuration

Most of the interesting plugin's configuration you want to expose to end users with configuration-as-code is managed by your plugin's 
Descriptor(s) and exposed on web UI with a `global.jelly` view. This is fully supported by configuration-as-code as long as you rely on
the exact same `DataBound` mechanism, which isn't a common practice (yet).

In many plugins, `Descriptor#configure()` is implemented by lookup for attributes values from the `JSONObject`. To make your Descriptor 
compliant with configuration-as-code, you'll need to expose your configuration attributes as `@DataBoundSetters`.

Here's the recommended approach :

Let's consider this Descriptor :

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

with global.jelly view :
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

Define `@DataBoundSetters` javabean setters for your Descriptor's properties. They should match the getters you already have for 
global.jelly data-binding.
   
```java   
   @DataBoundSetter
   public void setCharset(String charset) {
       this.charset = charset;
   }     
```     
   
### Step 2   
Create a new Describable object with a `config.jelly` view to own optional attributes. 

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
      <f:entry title="${%Password}" field="smtpAuthPassword">
        <f:password />
      </f:entry>
</j:jelly>
```

### Step 3
Define a new attribute in your Descriptor to own optional attributes. 
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

Replace `optionalBlocks` in your jelly view with `optionalProperty` and add the required DataBound accessors
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
default values as a Desciptor is a mutable object, i.e. data-binding won't reset values if they are not present in JSON payload.

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
If you don't have one already, define a `@Symbol` annotation on your descriptor. This is the name end-user will be able to use to access
your Descriptor for configuration. To avoid collisions with other plugin, prefer using your plugin's artifactId as a symbolic name for your
descriptor.

```java   
@Symbol("foo")
public static final class DescriptorImpl extends Descriptor<Foo> {
```

See [mailer plugin#39](https://github.com/jenkinsci/mailer-plugin/pull/39) for a sample on required changes.
