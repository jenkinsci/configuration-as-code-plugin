## Triggering Configuration Reload

You have the following option to trigger a configuration reload:

- via the user interface: `Manage Jenkins -> Configuration -> Reload existing configuration`
- via http POST to `JENKINS_URL/configuration-as-code/reload`
  Note: this needs to include a valid CRUMB and authentication information e.g. username + token of a user with admin
  permissions. Since Jenkins 2.96 CRUMB is not needed for API tokens.
- via Jenkins CLI
- via http POST to `JENKINS_URL/reload-configuration-as-code`
  It's disabled by default and secured via a token configured as system property `casc.reload.token`.
  Setting the system property enables this functionality and the requests need to include the token as
  query parameter named `casc-reload-token`, i.e. `JENKINS_URL/reload-configuration-as-code/?casc-reload-token=32424324rdsadsa`.

  `curl  -X POST "JENKINS_URL:8080/reload-configuration-as-code/?casc-reload-token=32424324rdsadsa"`

- via Groovy script
  ```groovy
  import io.jenkins.plugins.casc.ConfigurationAsCode;
  ConfigurationAsCode.get().configure()
  ```