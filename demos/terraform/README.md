# example of how to configure terraform plugin

to test it from root of the current repository:
```
export CASC_JENKINS_CONFIG=$(PWD)/demos/terraform/jenkins.yaml
mvn hpi:run
```

You need to install the following plugins to make it work (you can do it before using the export):
- Configuration As Code
- Configuration As Code Support
- Job DSL
- SSH Credentials
