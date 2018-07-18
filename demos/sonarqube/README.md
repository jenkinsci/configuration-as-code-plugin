# configure sonar plugin

## sample configuration
Sample configuration for the SonarQube plugin

```yaml
jenkins:
  [...]
unclassified:
  sonarglobalconfiguration:     # mandatory
    installations:              # mandatory
      - name: Sonar          # id of the SonarQube configuration - to be used in jobs
        serverUrl: http://SERVER_URL/
        serverAuthenticationToken: sonarAuthenticationToken # Authentication token for SonarQube
```

## notes
You can add multiple installations.
