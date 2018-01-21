# configure jenkins

Basic Jenkins configuration, which is not a part of any plugin

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