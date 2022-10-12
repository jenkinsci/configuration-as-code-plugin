# Configure Maven

Basic configuration of [Maven](https://plugins.jenkins.io/maven-plugin/)

## automated tool installation on agents

```yaml
tool:
  maven:
    installations:
      - name: maven3
        home: "/maven3"
        properties:
          - installSource:
              installers:
                - maven:
                    id: "3.8.4"
```

## global configuration

### Using Maven default files

```yaml
tool:
  mavenGlobalConfig:
    globalSettingsProvider: "standard"
    settingsProvider: "standard"
```

### Using given files

```yaml
tool:
  mavenGlobalConfig:
    globalSettingsProvider:
      filePath:
        path: "/conf/maven/global-settings.xml"
    settingsProvider:
      filePath:
        path: "/conf/maven/settings.xml"
```

### Using a configured config file

This use the [Config File Provider](https://plugins.jenkins.io/config-file-provider/) plugin

```yaml
credentials:
  system:
    domainCredentials:
    - credentials:
      - usernamePassword:
          description: "your mirror credentials for Jenkins"
          id: "your-mirror-creds"
          password: "${JENKINS_PASSWORD}"
          scope: GLOBAL
          username: "jenkins"

unclassified:
  globalConfigFiles:
    configs:
    - globalMavenSettings:
        comment: "Maven Global settings"
        content: |
          <?xml version="1.0" encoding="UTF-8"?>
          <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
            <pluginGroups />
            <proxies />
            <mirrors>
              <mirror>
                <id>your-mirror</id>
                <mirrorOf>*</mirrorOf>
                <name>your mirror</name>
                <url>https://your-mirror/</url>
              </mirror>
            </mirrors>
          </settings>
        id: "global-maven-settings"
        isReplaceAll: true
        name: "global-maven-settings"
        providerId: "org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig"
        serverCredentialMappings:
        - credentialsId: "your-mirror-creds"
          serverId: "your-mirror"

tool:
  mavenGlobalConfig:
    globalSettingsProvider:
      mvn:
        settingsConfigId: "global-maven-settings"
    settingsProvider: "standard"
```
