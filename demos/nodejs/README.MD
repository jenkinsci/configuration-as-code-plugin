# Configure NodeJS

Basic configuration of [NodeJS](https://plugins.jenkins.io/nodejs)

## Sample configuration

```yaml
tool:
  nodejs:
    installations:
      - name: "NodeJS Latest"
        properties:
          - installSource:
              installers:
                - nodeJSInstaller:
                    id: "12.11.1"
                    npmPackagesRefreshHours: 72
```
