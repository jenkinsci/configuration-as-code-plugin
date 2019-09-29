# Configure JDK

Basic configuration of the [JDK](https://plugins.jenkins.io/jdk-tool)

## sample configuration

```yaml
tool:
  jdk:
    installations:
      - name: jdk8
        home: "/jdk"
        properties:
          - installSource:
              installers:
                - jdkInstaller:
                    id: "jdk-8u181-oth-JPR"
                    acceptLicense: true
```
