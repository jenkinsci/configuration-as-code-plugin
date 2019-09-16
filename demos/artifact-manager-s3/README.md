# Configure artifact manager s3

Basic configuration of the [Artifact Manager on S3 plugin](https://plugins.jenkins.io/artifact-manager-s3)

## sample configuration

```yaml
unclassified:
  artifactManager:
    artifactManagerFactories:
      - jclouds:
          provider: s3

aws:
  awsCredentials:
    region: "us-east-1"
  s3:
    container: "${ARTIFACT_MANAGER_S3_BUCKET_NAME}"
    prefix: "jenkins_data/"
```
