# configure sonar plugin

## sample configuration
Sample configuration for the SonarQube plugin

```yaml
jenkins:
  [...]


unclassified:
  sonarglobalconfiguration:     # mandatory
    buildWrapperEnabled: true
    installations:              # mandatory
      - name: sonarqube          # id of the SonarQube configuration - to be used in jobs
        serverUrl: http://sonarqube-service:9000/sq
        additionalAnalysisProperties:
        additionalProperties:
        triggers:
```

## notes
You can add multiple installations.
