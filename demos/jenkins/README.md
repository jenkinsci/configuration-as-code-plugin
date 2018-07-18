# configure jenkins

Basic Jenkins configuration under `Configure System`, which is not a part of any plugin

Many of the plugins are actually configured in the same section, but to configure them you'll put their configuration under `unclassified` root element - details in plugin's specific subfolders.

[jenkins.yaml](jenkins.yaml) file is an example of configuration file with Jenkins and a number of plugins configured.


## sample configuration

```yaml
jenkins:
  systemMessage: "Jenkins configured automatically by Jenkins Configuration as Code Plugin\n\n"
  numExecutors: 5
  scmCheckoutRetryCount: 2
  mode: NORMAL
```

### Multiline system message
There are (too) many ways to write multi-line strings in yaml, but one of the most readable solutions
is to use the following syntax, that doesn't need escaped newlines and other shenanigans:

```yaml
jenkins:
  systemMessage: |
    Welcome to our build server.

    This Jenkins is 100% configured and managed 'as code'.
    Config is now mostly handled by 'Jenkins Configuration as Code Plugin' (JCasC).
    JCasC config can be found in the jenkins.yaml file in the $JENKINS_HOME/casc/ folder.

    some settings are still injected from init.groovy.d scripts,
    but these settings will be ported over to JCasC as support becomes available.
  numExecutors: 1  # This is just a random example entry to show that there is no "end token" for the multiline string apart from un-indent to the next yaml property.
```

# implementation note
Example above is only a subset of commonly used settings, full list available in generated documentation
