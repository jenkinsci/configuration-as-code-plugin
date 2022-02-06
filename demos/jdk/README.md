# Configure JDK

Basic configuration of the [JDK](https://plugins.jenkins.io/jdk-tool), using [AdoptOpenJDK installer](https://plugins.jenkins.io/adoptopenjdk/)

## sample configuration

```yaml
tool:
  jdk:
    installations:
      - name: jdk11
        home: "/jdk"
        properties:
          - installSource:
              installers:
                - adoptOpenJdkInstaller:
                    id: "jdk-11.0.14+9"
```
