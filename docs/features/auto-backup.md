This feature provides a solution to allow users to upgrade their Jenkins Configuration-as-Code config file.

## Use case

For the users who wants to build a Jenkins distribution, configuration-as-code could be a good
 option to provide a initial configuration which lets Jenkins has the feature of out-of-the-box.
 
But there's one problem here, after the Jenkins distribution runs for a while. User must wants to
 change the configuration base on his use case. So there're two YAML config files needed.
 One is the initial one which we call it `system.yaml` here, another one belongs to user's data
 which is `user.yaml`.
 
The behaviour of generating the user's configuration automatically is still
 [working in progress](https://github.com/jenkinsci/configuration-as-code-plugin/pull/1218).
 
## How does it work?

First, check if there's a new version of the initial config file which is
 `${JENKINS_HOME}/war/jenkins.yaml`. If there isn't, skip all the following steps.
 
Second, check if there's a user data file. If it exists, than calculate the diff between
 the previous config file and the user file. Or just replace the old file simply and skip
 all the following steps.
 
Third, apply the patch into the new config file as the result of user file.

Finally, replace the old config file with the new one and delete the new config file.

We deal with three config files:

|Config file path|Description|
|---|---|
|`${JENKINS_HOME}/war/jenkins.yaml`|Initial config file, put the new config files in here|
|`${JENKINS_HOME}/war/WEB-INF/jenkins.yaml`|Should be the last version of config file|
|`${JENKINS_HOME}/war/WEB-INF/jenkins.yaml.d/user.yaml`|All current config file, auto generate it when a user change the config|

## TODO

- let the name of config file can be configurable
