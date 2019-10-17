# Configure statistics-gatherer plugin

Sample configuration for the [Statistics Gatherer plugin](https://plugins.jenkins.io/statistics-gatherer).

## sample configuration

Sample configuration for the Statistics Gatherer plugin.

```yaml
unclassified:
  statisticsconfiguration:
    buildUrl: "http://elasticsearch:9200/jenkins-stats/builds"
    shouldSendApiHttpRequests: true
    buildInfo: true
    queueInfo: false
    projectInfo: false
    buildStepInfo: false
    scmCheckoutInfo: true
```
