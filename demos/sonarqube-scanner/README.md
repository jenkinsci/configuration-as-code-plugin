# Configure SonarQube Scanner

Basic configuration of [SonarQube Scanner](https://plugins.jenkins.io/sonar/)

## Sample configuration

```yaml
tool:
  sonarRunnerInstallation:
    installations:
    - name: "SonarQube Scanner"
      home: ""
      properties:
      - installSource:
          installers:
          - sonarRunnerInstaller:
              id: "4.7.0.2747"
```
