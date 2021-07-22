# Configure Jira

Basic configuration of the [Synopsys(BlackDuck)](https://plugins.jenkins.io/blackduck-detect/)

## sample configuration

```yaml
unclassified:
  detectGlobalConfig:
    blackDuckCredentialsId: ""
    blackDuckTimeout: 120
    blackDuckUrl: ""
    downloadStrategy: "scriptOrJarDownloadStrategy"
    trustBlackDuckCertificates: true