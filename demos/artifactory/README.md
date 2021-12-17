# artifactory plugin

Artifactory plugin configuration belongs under `unclassified` root element

## sample configuration

Since 3.11.0:

```yaml
unclassified:
  artifactorybuilder:
    useCredentialsPlugin: true
    jfrogInstances:
      - instanceId: artifactory
        platformUrl: http://acme.com/artifactory
        artifactoryUrl: http://acme.com/artifactory
        distributionUrl: http://acme.com/distribution
        deployerCredentialsConfig:
          credentialsId: "artifactory"
        resolverCredentialsConfig:
          username: artifactory_user
          password: "${ARTIFACTORY_PASSWORD}"
```

Before 3.11.0:

```yaml
unclassified:
  artifactorybuilder:
    useCredentialsPlugin: true
    artifactoryServers:
      - serverId: artifactory
        artifactoryUrl: http://acme.com/artifactory
        deployerCredentialsConfig:
          credentialsId: "artifactory"
        resolverCredentialsConfig:
          username: artifactory_user
          password: "${ARTIFACTORY_PASSWORD}"
```

## implementation note

Currently setting credentials causes ERROR & `Enable Push to Bintray` is not supported (always enabled).

see [jfrog/HAP-1018](https://www.jfrog.com/jira/browse/HAP-1018)
