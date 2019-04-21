# Configure seed jobs

As explained in [seed-jobs.md](../../docs/seed-jobs.md), `jobs` declaration is useful to create an initial set of jobs.

For now, it is using the [job-dsl-plugin](https://wiki.jenkins.io/display/JENKINS/Job+DSL+Plugin) so this plugin needs to be installed on your Jenkins instance for this sample to work.

The Job DSL plugin uses groovy syntax for its job configuration DSL, so a mix of YAML and groovy must be used within the configuration-as-code file.

## sample configurations

[bitbucket.yaml](bitbucket.yaml) file is an example of a configuration file with Jenkins and an Organization Folder Job Type with automatic branch discovering in Bitbucket. It requires [Branch API plugin](https://github.com/jenkinsci/branch-api-plugin) and [Bitbucket Branch Source plugin](https://github.com/jenkinsci/bitbucket-branch-source-plugin) to be able to run this demo. `$BITBUCKET_URL` is a system environment variable that needs to be defined before Jenkins is started.

[pipeline.yaml](pipeline.yaml) file is an example of configuring a folder and a declarative pipeline job within that folder.

[multibranch-github.yaml](multibranch-github.yaml) file is an example of a multibranch pipeline job configured with GitHub as branch source, an orphaned item strategy and periodic scan triggers of 5 mins.

## implementation note

The main issue with the `jobs` declaration for now is the difference in the `Traits` declaration due to [JENKINS-45504](https://issues.jenkins.io/browse/JENKINS-45504). When is is resolved, the workaround using the `configure` part will no longer be needed and all traits will be declared under the organizations section.

Job DSL only allows `periodic(int min)` for configuring a trigger for now. So to configure "1 day" for example, we need to use the `configure` workaround as shown in [bitbucket.yaml](bitbucket.yaml#L68)
