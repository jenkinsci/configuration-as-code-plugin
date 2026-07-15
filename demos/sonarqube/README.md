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

## SonarScanner tool installation

Jenkins can either install SonarScanner automatically on agents or use a scanner that is already installed on them.

### Automatic installation

When a pipeline requests this tool, Jenkins provisions the selected SonarScanner version on the agent where the analysis runs.

```yaml
tool:
  sonarRunnerInstallation:
    installations:
      - name: "SonarScanner"
        properties:
          - installSource:
              installers:
                - sonarRunnerInstaller:
                    id: "7.3.0.5189"
```

### Pre-installed scanner

Use this option when SonarScanner is already available at the same path on every agent that runs the analysis. For Kubernetes agents, the scanner can be included in the relevant agent container image. Jenkins does not download SonarScanner when this option is used.

```yaml
tool:
  sonarRunnerInstallation:
    installations:
      - name: "SonarScanner"
        home: "/opt/sonar-scanner"
```

## notes

You can add multiple installations.
