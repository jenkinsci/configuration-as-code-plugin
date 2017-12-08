# TFS/Team Services plugin

## sample configuration

```yaml

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
