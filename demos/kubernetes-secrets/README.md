# Configure Kubernetes secrets for Jenkins Configuration as Code plugin

### Prerequisites

1. `SECRETS` environment variable should provide a path to mounted secret volume.
2. Kubernetes secrets with all required values.
3. `volumeMounts` and `volumes` directives of Kubernetes manifest should have records for Kubernetes secrets mounts.

### Sample configuration

```
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: jenkins-casc
data:
  jenkins.yaml: |
    jenkins:
      location:
        url: http://jenkins/
        adminAddress: "${SECRET_JENKINS_ADMIN_ADDRESS}"
---
kind: Secret
apiVersion: v1
metadata:
  name: jenkins-secrets
type: Opaque
data:
  # All values for secrets should be provided in base64 encoding
  SECRET_JENKINS_ADMIN_ADDRESS: YWRtaW5AZXhhbXBsZS5jb20=
---
apiVersion: apps/v1beta1
kind: StatefulSet
...
      spec:
        containers:
          - name: jenkins
            ...
            env:
              # Read the configuration-as-code from the ConfigMap
              - name: CASC_JENKINS_CONFIG
                value: /var/jenkins_config/jenkins.yaml
              # With the help of SECRETS environment variable
              # we point Jenkins Configuration as Code plugin the location of the secrets
              - name: SECRETS
                value: /secrets/jenkins
            ...
            # Mount the configuration-as-code ConfigMap
            volumeMounts:
              - name: jenkins-configuration-as-code
                mountPath: /var/jenkins_config
              - name: jenkins-secrets
                mountPath: /secrets/jenkins
                readOnly: true
            ...
        volumes:
          # The configuration-as-code ConfigMap
          - name: jenkins-configuration-as-code
            configMap:
              name: jenkins-casc
          - name: jenkins-secrets
            secret:
              secretName: jenkins-secrets
```
