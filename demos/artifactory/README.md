# artifactory plugin

## sample configuration

```yaml
artifactorybuilder:
  useCredentialsPlugin: true
  artifactoryServers:
    - name: foo
      serverId: artifactory
      artifactoryUrl: http://acme.com/artifactory
      resolverCredentialsConfig:
        username: artifactory_user
        password: SECRET
```

## implementation note
Currently setting credentials causes ERROR & `Enable Push to Bintray` is not supported (always enabled).

see [https://www.jfrog.com/jira/browse/HAP-1018]

