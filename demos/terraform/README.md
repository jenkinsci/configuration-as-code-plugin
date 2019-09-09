# example of how to configure terraform plugin

## Prerequisites

to test it from root of the current repository:

```bash
export CASC_JENKINS_CONFIG=$(PWD)/demos/terraform/jenkins.yaml
mvn hpi:run
```

You need to install the following plugins to make it work (you can do it before using the export):

- Configuration As Code
- Configuration As Code Support
- Job DSL
- SSH Credentials

## Sample

```yml
jenkins:
  systemMessage: "Jenkins configured automatically by Jenkins Configuration as Code plugin\n\n"

tool:
  terraforminstallation:
    installations:
      - name: terraform
        home: ""
        properties:
          - installSource:
              installers:
                - terraformInstaller:
                    id: "0.11.9-linux-amd64"

jobs:
  - script: >
      job("terraform-job") {
        description()
        keepDependencies(false)
        disabled(false)
        concurrentBuild(false)
        wrappers {
           terraformBuildWrapper {
              variables("") 
              terraformInstallation("terraform")
              doGetUpdate(true)
              doNotApply(false)
              doDestroy(false)
              config {
                 value("inline")
                 inlineConfig("")
                 fileConfig("")
              }
           }
        }
      }
```
