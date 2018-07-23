# Configure seed jobs 

As explained in [seed-jobs.md](../../docs/seed-jobs.md), `jobs` declaration is useful to create an initial set of jobs. 

For now, it is using the [job-dsl-plugin](https://wiki.jenkins.io/display/JENKINS/Job+DSL+Plugin) so this plugin needs to be installed on your Jenkins instance for this sample to work.

Job-DSL plugin uses groovy syntax for it's job configuration DSL, so a mix of yaml and groovy must be used within the
configuration-as-code file.

## sample configuration

[bitbucket.yaml](bitbucket.yaml) file is an example of configuration file with Jenkins and an Organization Folder Job Type with automatic branch discovering in Bitbucket.

## implementation note

- The main issue in `jobs` declaration for now concerns the difference in 'Traits' declaration due to https://issues.jenkins-ci.org/browse/JENKINS-45504.
When is is resolved, the workaround using `configure` part will no longer be needed and all traits will be declared under the organizations section.

- JobDSL only allow 'periodic(int min)' for configuring trigger for now. So to configure "1 day" for example, we need to use the `configure` workaround as shown in [bitbucket.yaml](bitbucket.yaml#L68) 
