# Configure Git plugin

Basic global configuration of the [Git plugin](https://plugins.jenkins.io/git).
Detailed descriptions of each option are available in the [git plugin documentation](https://plugins.jenkins.io/git/#plugin-content-global-configuration).

## sample configuration

```yaml
unclassified:
  scmGit:
    addGitTagAction: false
    allowSecondFetch: false
    createAccountBasedOnEmail: true
    disableGitToolChooser: false
    globalConfigEmail: jenkins@domain.local
    globalConfigName: jenkins
    hideCredentials: false
    showEntireCommitSummaryInChanges: true
    useExistingAccountWithSameEmail: false
```
