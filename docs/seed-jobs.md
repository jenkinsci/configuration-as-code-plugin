# How to create initial "seed" job

Configuration is not just about setting up Jenkins master, it's also about creating an initial set of jobs.
For this purpose, we delegate to the popular [job-dsl-plugin](https://wiki.jenkins.io/display/JENKINS/Job+DSL+Plugin)
and run a job-dsl script to create an initial set of jobs.

Be aware that you also need [configuration-as-code-support](https://plugins.jenkins.io/configuration-as-code-support) besides [configuration-as-code](https://plugins.jenkins.io/configuration-as-code) plugin already installed, otherwise the `jobs` root element cannot be parsed.

Typical usage is to rely on a multi-branch, or organization folder job type, so further jobs will be dynamically
created. So a multi-branch seed job will prepare a master to be fully configured for CI/CD targeting a repository
or organization.

Job-DSL plugin uses groovy syntax for its job configuration DSL, so you'll have to mix YAML and groovy within your
configuration-as-code file:

```yaml
jenkins:
  systemMessage: "Simple seed job example"
jobs:
  - script: >
      multibranchPipelineJob('configuration-as-code') {
          branchSources {
              git {
                  id = 'configuration-as-code'
                  remote('https://github.com/jenkinsci/configuration-as-code-plugin.git')
              }
          }
      }
```

## Examples

Please refer to [demos](../demos/jobs) for examples to configure more complex jobs.
