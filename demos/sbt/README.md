# Configure sbt plugin

Basic configuration of the [Sbt plugin](https://plugins.jenkins.io/sbt)

## Sample configuration

```yaml
tool:
  sbtInstallation:
    installations:
      - name: sbt
        home: "/usr/bin/sbt"
        properties:
          - installSource:
              installers:
                - sbtInstaller:
                    id: "1.2.8"
```
