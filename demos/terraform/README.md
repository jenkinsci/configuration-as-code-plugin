# example of how to configure terraform plugin 

to test it from root of the current repository:
```
export CASC_JENKINS_CONFIG=$(PWD)/demos/terraform/jenkins.yaml
mvn hpi:run
```

You need to install the following plugins to make it work (you can do it before using the export):
- job DSL
- job configuration-as-code support
- SSH credentials
- job configuration-as-code 
