# configure sonar plugin

## sample configuration
Sample configuration for the SonarQube plugin

```yaml
unclassified:

  sonarglobalconfiguration:     # mandatory
    installations:              # mandatory
      - Sonar566:
        name: Sonar566          # id of the SonarQube configuration - to be used in jobs
        serverUrl: http://SERVER_URL/
        serverVersion: 5.3      # id of the combobox of sonar version
```

## notes
You can add multiple installations.

