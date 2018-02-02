# TFS/Team Services plugin

TFS plugin configuration belongs under `jenkins` root element

## sample configuration

```yaml
jenkins:
  [...]
  teampluginglobalconfig:
    collectionConfigurations:
      - collectionUrl: http://test.com
        credentialsId: tfsCredentials
    enableTeamPushTriggerForAllJobs: true
    enableTeamStatusForAllJobs: true
    #userAccountMapper:
    configFolderPerNode: true
```

## implementation note

User account name mapping strategy is not yet supported
