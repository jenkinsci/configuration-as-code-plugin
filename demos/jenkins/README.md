# configure jenkins

Basic Jenkins configuration under `Configure System`, which is not a part of any plugin

Many of the plugins are actually configured in the same section, so to configure them you'll put their configuration also under `jenkins` root element - details in plugin specific subfolders.

[jenkins.yaml](jenkins.yaml) file is an example of configuration file with Jenkins and a number of plugins configured.


## sample configuration

```yaml
jenkins:
  systemMessage: "Jenkins configured automatically by Jenkins Configuration as Code Plugin\n\n"
  numExecutors: 5
  scmCheckoutRetryCount: 2
  mode: NORMAL
  scmCheckoutRetryCount: 4
```

# implementation note  
Example above is only a subset of commonly used settings, full list available in generated documentation
