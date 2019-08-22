# configure sonar plugin

## sample configuration

Sample configuration for the SonarQube plugin.

*Below sample configuration willingly set all attributes values because of current issues with Sonar plugin 2.9 version. (cf. #982)*

```yaml
unclassified:
  sonarglobalconfiguration:             # mandatory
    buildWrapperEnabled: true
    installations:                      # mandatory
      - name: sonarqube                 # id of the SonarQube configuration - to be used in jobs
        serverUrl: http://sonarqube-service:9000/sq
        credentialsId: token-sonarqube  # id of the credentials containing sonar auth token (since 2.9 version)
        serverAuthenticationToken: ""   # for retrocompatibility with versions < 2.9
        additionalAnalysisProperties:
        additionalProperties:
        mojoVersion:
        triggers:
          envVar:
          skipScmCause: false
          skipUpstreamCause: false
```

## notes

You can add multiple installations.
