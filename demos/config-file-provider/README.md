# configure config-file-provider files

support for Configuration as Code is available from config-file-provider 3.4.1.

## sample configuration

```yaml
unclassified:
  globalConfigFiles:
    configs:
      - custom:
          id: custom-test
          name: DummyCustom1
          comment: dummy custom 1
          content: dummy content 1
      - json:
          id: json-test
          name: DummyJsonConfig
          comment: dummy json config
          content: |
            { "dummydata": {"dummyKey": "dummyValue"} }
      - xml:
          id: xml-test
          name: DummyXmlConfig
          comment: dummy xml config
          content: <root><dummy test="abc"></dummy></root>
      - mavenSettings:
          id: maven-test
          name: DummySettings
          comment: dummy settings
          isReplaceAll: false
          serverCredentialMappings:
            - serverId: server1
              credentialsId: someCredentials1
            - serverId: server2
              credentialsId: someCredentials2
          content: |
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                      xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
              <!-- activeProfiles
               | List of profiles that are active for all builds.
               |
              -->
              <activeProfiles>
                <activeProfile>alwaysActiveProfile</activeProfile>
                <activeProfile>anotherAlwaysActiveProfile</activeProfile>
              </activeProfiles>
            </settings>
```
