# configure global libraries plugin

Global Pipeline Libraries plugin configuration belongs under `jenkins` root element

## sample configuration

```yaml
jenkins: [...]
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
