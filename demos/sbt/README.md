# Configure sbt plugin

Basic configuration of the Sbt plugin

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
                    id: '1.2.8'
```
