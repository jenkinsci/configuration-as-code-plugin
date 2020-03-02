# Configure log recorders

Requires jenkins >= 2.224

## Sample configuration

```yaml
jenkins:
  log:
    recorders:
    - name: "JCasC"
      targets:
      - level: "WARNING"
        name: "io.jenkins.plugins.casc"
```
