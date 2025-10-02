# Exporting configurations

The plugin supports exporting existing configurations as YAML.
This can be achieved with the following options:

* Accessing the `http://[your_jenkins_url]/configuration-as-code/` URL as a Jenkins administrator, and clicking `Export configuration`
* Running the following in a Groovy script (not recommended, uses internal APIs):

      import io.jenkins.plugins.casc.ConfigurationAsCode
      def stream = new ByteArrayOutputStream()
      ConfigurationAsCode.get().export(stream)
      println stream.toString()

Export may not offer a directly usable jenkins.yaml configuration.
It is normally better when you copy the relevant sections you need instead of the entire file.

## Security notice

Jenkins configuration may include various sensitive information,
including, but not limited to, credentials, secrets, administrative information about the instance and user personal data.
The Configuration-as-Code plugin tracks secrets and represents them safely in the exported YAMLs,
but it cannot prevent secrets from being exported in all cases.
Ultimately, it is the responsibility of Jenkins administrators to ensure that the generated YAML files
do not include sensitive information.

See more information about the masking logic below.

## Data to be exported

The plugin does not have a way to define which data should be exported.
The following data is exported:

* System configuration under the _Manage Jenkins_ link 
  (global configurations, descriptor configurations, etc.)
* Agent configurations
* Views
* Credentials
* Users - Only if using the security realm "Jenkins’ own user database"

The plugin does NOT export Jobs.

## Secret masking

What will be masked:

* All [hudson.util.Secret](https://javadoc.jenkins-ci.org/hudson/util/Secret.html) attributes
  are exported in the encrypted form
  * Encrypted form is readable only on the same instance, 
    and hence the encrypted form cannot be restored from the file on other instances
    (see [this article](http://xn--thibaud-dya.fr/jenkins_credentials.html))
* Credential and secret definitions which use `hudson.util.Secret`
  internally. 
  All plugins are expected to do so ([documentation](https://jenkins.io/doc/developer/security/secrets/))
  
What will **NOT** be masked:

* Free-form fields like view and agent descriptions. 
  If they contain sensitive information, it will be exported.
  * NOTE: Depending on the permission setup these items may be configured by non-admin users,
    Jenkins admins should keep it in mind while exporting configurations
* Plugins which do not use `hudson.util.Secret` for handling secret data.
  For example, unfixed plugins in [this advisory](https://jenkins.io/security/advisory/2019-04-03/)
  are likely subject to this issue
