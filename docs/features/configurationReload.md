## Triggering Configuration Reload

You have the following option to trigger a configuration reload:

- via the user interface: `Manage Jenkins -> Configuration -> Reload existing configuration`
- via http POST to `JENKINS_URL/reload-configuration-as-code/` (note the trailing slash).  
    It's disabled by default and secured via a token configured either by the environment variable
    `CASC_RELOAD_TOKEN` or the system property `casc.reload.token`. If both are set, the environment
    variable takes precedence.  
    Configuring this token enables this functionality and the requests need to include it as a
    query parameter named `casc-reload-token`, e.g. `JENKINS_URL/reload-configuration-as-code/?casc-reload-token=someSecretValue`.

```sh
$ curl -X POST "JENKINS_URL/reload-configuration-as-code/?casc-reload-token=someSecretValue"

# To avoid showing the token on the process list, one can use read the parameter from a secure file:
$ cat /path/to/secret/file
casc-reload-token=someSecretValue
$ curl -X POST -G -d @/path/to/secret/file "JENKINS_URL/reload-configuration-as-code/"

```
- via http POST to `JENKINS_URL/configuration-as-code/reload`
  Note: this needs to include a valid CRUMB and authentication information e.g. username + token of a user with admin
  permissions. Since Jenkins 2.96 CRUMB is not needed for API tokens.
- via [Jenkins CLI](https://www.jenkins.io/doc/book/managing/cli/): with the Jenkins CLI (either with SSH or JAR), the command `java -jar jenkins-cli.jar -s ${JENKINS_URL} reload-jcasc-configuration` triggers a configuration reload.
  This Jenkins CLI command is only present when the plugin `configuration-as-code` is installed, and reported in the help message:
  
```shell
$ java -jar jenkins-cli.jar -s ${JENKINS_URL} help
# ...
reload-jcasc-configuration
    Reload JCasC YAML configuration
# ...
```

- via Groovy script (not recommended)
  ```groovy
  import io.jenkins.plugins.casc.ConfigurationAsCode
  ConfigurationAsCode.get().configure()
  ```
  _Note: that running the above code in a pipeline will put this plugin in a bad state where the configuration cannot be reloaded at all until Jenkins is restarted. See [#1227](https://github.com/jenkinsci/configuration-as-code-plugin/issues/1227) for more info._
