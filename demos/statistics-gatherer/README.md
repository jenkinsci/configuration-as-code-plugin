# configure statistics-gatherer plugin

## sample configuration

Sample configuration for the Statistics Gatherer plugin.

```yaml
jenkins:
  [...]

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
