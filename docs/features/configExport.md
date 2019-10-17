# Exporting configurations

The plugin supports exporting existing configurations as YAML.
This is a feature available to the Jenkins administrators under the `http://[your_jenkins_url]/configuration-as-code/` URL.

Export feature is **NOT** intended to offer a directly usable jenkins.yaml configuration. 
It can be used for inspiration writing your own production-ready YAML, but be aware that export can be partial, 
or fail for some components.

## Security notice

Jenkins configuration may include various sensitive information,
including, but not limited to, credentials, secrets, administrative information about the instance and user personal data.
The Configuration-as-Code plugin tracks secrets and represents them safely in the exported YAMLs,
but it cannot prevent secrets from being exported in all cases.
Ultimately, it is a responsibility of Jenkins administrators to ensure that the generated YAML files
do not include sensitive information.

See more information about the masking logic below.

## Data to be exported

Currently the plugin does not have a way to define which data should be exported.
The following data is exported:

* System configuration under the _Manage Jenkins_ link 
  (global configurations, descriptor configurations, etc.)
* Agent configurations
* Views
* Credentials

Jobs and users are NOT exported by the plugin.

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
