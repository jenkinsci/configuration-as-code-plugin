# Configure ThinBackup

Requires `thin-backup` >= 2.0
Basic configuration of [ThinBackup](https://plugins.jenkins.io/thinBackup/) plugin.

## Sample configuration

```yaml
unclassified:
  thinBackup:
    backupAdditionalFiles: false
    backupAdditionalFilesRegex: "^.*\\.(txt)$"
    backupBuildArchive: false
    backupBuildResults: true
    backupBuildsToKeepOnly: false
    backupConfigHistory: false
    backupNextBuildNumber: false
    backupPath: "c:\\temp\\thin-backup"
    backupPluginArchives: false
    backupUserContents: false
    cleanupDiff: false
    diffBackupSchedule: "0 12 * * 1-5"
    excludedFilesRegex: "^.*\\.(log)$"
    failFast: true
    forceQuietModeTimeout: 120
    fullBackupSchedule: "0 12 * * 1"
    moveOldBackupsToZipFile: false
    nrMaxStoredFull: -1
    waitForIdle: true
```
