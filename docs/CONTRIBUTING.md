# Contribution guide

**Never report security issues on GitHub or other public channels (Gitter/Twitter/etc.), follow the instruction from [Jenkins Security](https://jenkins.io/security/) to report it on [Jenkins Jira](https://issues.jenkins-ci.org)**

## Reporting plugin compatibility issues

See [COMPATIBILITY](./COMPATIBILITY.md)

## Why should you contribute

You can contribute in many ways, and whatever you choose we're grateful!
Source code contribution is the obvious one but we also need your feedback and if you don't really want to participate in the implementation directly you may still have great ideas about features we need (or should get rid of).

We have our vision for the plugin and we have an experience with maintaining Jenkins instances, but the plugin is not supposed to solve only our problems. Surely we haven't experienced all of them... That's why we want to hear from you.

Please use GitHub issues if you need to report a bug or request changes/improvements.
Whenever you report a problem please provide information about:

- Plugin version (Configuration as Code plugin as well any other plugin you suspect your problem to be related to)
- Jenkins version
- Operating system
- Description!

### Regarding source code contribution WoW

- Create a GitHub issue for your feature/problem you want to solve
- Implement solution on a branch in your fork
- Make sure to include issue number in commit message, and make the message speak for itself
- Once you're done create a pull request and ask at least one of the maintainers for review
  - Remember to title your pull request properly as it is used for release notes

Never push directly to this repository!

### Newbie-friendly issues

If you are just starting with contribution to Jenkins, 
we have identified some newbie-friendly issues related to Configuration-as-Code. 
Here are some queries you can use to find such issues:

- [GitHub Issues](https://github.com/jenkinsci/configuration-as-code-plugin/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22)
- [Jenkins JIRA Query](https://issues.jenkins-ci.org/issues/?filter=18649&jql=project%20%3D%20JENKINS%20AND%20status%20in%20(Open%2C%20Reopened)%20AND%20labels%20%3D%20newbie-friendly%20AND%20(labels%20in%20(jcasc-compatibility%2C%20jcasc-devtools-compatibility)%20or%20component%20in%20(configuration-as-code-plugin%2C%20configuration-as-code-groovy-plugin%2C%20configuration-as-code-secret-ssm-plugin)%20))

Somebody keeps fixing these issues all the time 😱. If the lists are empty,
just ask in [our Gitter Channel](https://gitter.im/jenkinsci/configuration-as-code-plugin).

### Project meetings

[Meeting minutes](https://docs.google.com/document/d/1Hm07Q1egWL6VVAqNgu27bcMnqNZhYJmXKRvknVw4Y84/edit?usp=sharing) and [meeting recordings](https://www.google.com/url?q=https://www.youtube.com/playlist?list%3DPLN7ajX_VdyaNgevVZbfczE4IeGifW-t87&sa=D&usd=2&usg=AOvVaw0QPw6eDS-jw_DgHgOaft3Z) of Jenkins Configuration as Code office hours are available for reference.
See our [Gitter chat](https://gitter.im/jenkinsci/configuration-as-code-plugin) for online chat.

## Run Locally

Prerequisites: _Java_, _Maven_ & _IntelliJ IDEA_

- Ensure Java 11 or 17 is available.

  ```shell
  java --version
  ```

- Ensure Maven is included in the PATH environment variable.

  ```shell
  export PATH=$PATH:/path/to/apache-maven-3.8.6/bin
  ```
  
  ### IntelliJ IDEA

- Open the root directory of this project in IntelliJ IDEA.
- If you are opening the first time, wait patiently while project dependencies are being downloaded.
- Click `Run` in the menu. Select `Edit Configurations` in the menu item.
- Click `Add New Configuration` (`+`) in the top left of the shown dialog. Select `Maven`.
- Under `Parameters` tab group, `Working directory:` is `/path/to/configuration-as-code-plugin/plugin`.
- Under `Parameters` tab group, `Command line:` is `hpi:run`.
- Verify that IntelliJ IDEA is not using bundled maven.
  - Click `File` -> `Preferences...` -> `Build, Execution, Deployment` -> `Build Tools` -> `Maven`.
  - `Maven home directory:` has `/path/to/apache-maven-x.y.z` value, not `Bundled (Maven 3)`.
- Open <http://localhost:8080/jenkins/configuration-as-code/> to test the plugin locally.

### CLI

- Go into the `plugin` child directory under the root directory of this project.
- Use the below commands.

```shell
export JAVA_HOME=`/usr/libexec/java_home -v 1.8`
export PATH=$PATH:/path/to/apache-maven-3.8.6/bin
mvn hpi:run
```

```text
...
INFO: Jenkins is fully up and running
```

- Open <http://localhost:8080/jenkins/configuration-as-code/> to test the plugin locally.
