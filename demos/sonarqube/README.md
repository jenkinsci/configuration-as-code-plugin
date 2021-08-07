# configure sonar plugin

## sample configuration

Sample configuration for the [SonarQube plugin](https://plugins.jenkins.io/sonar).

*Below sample configuration willingly set all attributes values because of current issues with Sonar plugin 2.9 version. (cf. #982)*

```yaml
credentials:
  system:
    domainCredentials:
      - credentials:
        - string:
            scope: GLOBAL
            id: "token"
            secret: "secret value"
            description: "Sonar token"

unclassified:
  sonarglobalconfiguration:                  # mandatory
    buildWrapperEnabled: true
    installations:                           # mandatory
      - name: "TEST"                         # id of the SonarQube configuration - to be used in jobs
        serverUrl: "http://url:9000"
        credentialsId: token       # id of the credentials containing sonar auth token (since 2.9 version)
        #serverAuthenticationToken: "token"   # for retrocompatibility with versions < 2.9
        mojoVersion: "mojoVersion"
        additionalProperties: "blah=blah"
        additionalAnalysisProperties: "additionalAnalysisProperties"
        triggers:
          skipScmCause: true
          skipUpstreamCause: true
          envVar: "envVar"
```

## notes

You can add multiple installations.
