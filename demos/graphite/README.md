# Configure Metrics Graphite plugin

Basic configuration of the [Metrics Graphite plugin](https://plugins.jenkins.io/metrics-graphite)

## Sample configuration

```yaml
unclassified:
  graphiteServer:
    servers:
    - hostname: "1.2.3.4"
      port: 2003
      prefix: "jenkins.master."
```
