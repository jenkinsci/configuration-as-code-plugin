# How to create initial "seed" job

Requires `job-dsl` >= 1.74

Configuration is not just about setting up Jenkins controller, it's also about creating an initial set of jobs.
For this purpose, we delegate to the popular [job-dsl-plugin](https://plugins.jenkins.io/job-dsl)
and run a job-dsl script to create an initial set of jobs.

Typical usage is to rely on a multi-branch, or organization folder job type, so further jobs will be dynamically
created. So a multi-branch seed job will prepare a controller to be fully configured for CI/CD targeting a repository
or organization.

Job-DSL plugin uses groovy syntax for its job configuration DSL, so you'll have to mix YAML and groovy within your
configuration-as-code file:

```yaml
jenkins:
  systemMessage: "Simple seed job example"
jobs:
  - script: |
      multibranchPipelineJob('configuration-as-code') {
          branchSources {
              git {
                  id = 'configuration-as-code'
                  remote('https://github.com/jenkinsci/configuration-as-code-plugin.git')
              }
          }
      }
```

Using Groovy scripting is also supported. Variables may be used with the `$varname` syntax. When using `${variable}` the variable is substituted by a credential, not by the Groovy variable. To use it nevertheless, the `$` sign may be escaped using `^`. See the following example:

```yaml
jenkins:
  systemMessage: "Seed job with loop."
jobs:
  - script: |
      jobarray = ['job1','job2']
      for(currentjob in jobarray)
      multibranchPipelineJob("$currentjob") { // normal variable syntax
          branchSources {
              git {
                  id = "^${currentjob}"       // accessing variable with escaping
                  remote('https://github.com/jenkinsci/configuration-as-code-plugin.git')
              }
          }
      }
```

## Examples

Please refer to [demos](../demos/jobs) for examples to configure more complex jobs.
