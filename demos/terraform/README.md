# Configure terraform plugin

Sample configuration for the [Terraform plugin](https://plugins.jenkins.io/terraform).

## Sample configuration

```yaml
tool:
  terraformInstallation:
    installations:
      - name: "terraform"
        home: "/terraform-0.11"
        properties:
          - installSource:
              installers:
                - terraformInstaller:
                    id: "0.11.9-linux-amd64"
```

An example of the job definition, with the JobDSL, that uses the terraform wrapper.

```yaml
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
