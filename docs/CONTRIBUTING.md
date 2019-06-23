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
