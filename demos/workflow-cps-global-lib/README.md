# Configure global libraries plugin

Global Pipeline Libraries plugin configuration belongs under the `unclassified` root element.
The [`configuration-as-code-support`] plugin needs to be installed as well.

## Sample configuration

```yaml
jenkins:
  [...]
unclassified:
  globalLibraries:
    libraries:
      - name: "awesome-lib"
        retriever:
          modernSCM:
            scm:
              git:
                remote: "https://github.com/jenkins-infra/pipeline-library.git"
```

## Using credentials

```yaml
jenkins:
  [...]
unclassified:
  globalLibraries:
    libraries:
      - name: 'internal-pipeline-library'
        retriever:
          modernSCM:
            scm:
              git:
                remote: 'git@github.com:furry-octo-lamp-inc/pipeline-library.git'
                credentialsId: 'reimagined-parakeet-ssh'
```

[`configuration-as-code-support`]: https://plugins.jenkins.io/configuration-as-code-support/
