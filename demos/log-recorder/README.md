# Configure log recorders

Requires jenkins >= 2.224

## Sample configuration

```yaml
jenkins:
  log:
    recorders:
    - name: "JCasC"
      loggers:
      - level: "WARNING"
        name: "io.jenkins.plugins.casc"
```
