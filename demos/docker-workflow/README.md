# Configure Declarative agent settings for Docker Pipeline

Global configuration for Docker Workflow plugin belongs under `unclassified` root element

## Sample configuration

```yaml
unclassified:
  pipeline-model-docker:
    dockerLabel: "label-casc"
    registry:
      url: "my.docker.endpoint"
      credentialsId: "credId"
```
