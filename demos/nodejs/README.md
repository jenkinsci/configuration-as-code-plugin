# Configure NodeJS

Basic configuration of [NodeJS](https://plugins.jenkins.io/nodejs)

## Sample configuration

```yaml
tool:
  nodejs:
    installations:
      - name: "NodeJS Latest"
        home: "" #required until nodejs-1.3.4 release (JENKINS-57508)
        properties:
          - installSource:
              installers:
                - nodeJSInstaller:
                    id: "12.11.1"
                    npmPackagesRefreshHours: 48 #default is 72
```
