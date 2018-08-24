# Configure Kubernetes plugin

Jenkins can be installed in Kubernetes and preconfigured to run jobs (and other
options) in the Kubernetes cluster, using yaml stored in a `ConfigMap`.
See [`config.yml`](config.yml) for the `ConfigMap` definition.

Example installation on Kubernetes:

```
kubectl apply -f service-account.yml
kubectl apply -f config.yml
kubectl apply -f jenkins.yml
```

## Sample configuration

```yaml
jenkins:
  location:
    url: http://jenkins/
  clouds:
    - kubernetes:
        name: kubernetes
        containerCapStr: 100
```

## Advanced sample configuration

```yaml
jenkins:
  location:
    url: http://jenkins/
  clouds:
    - kubernetes:
        name: "advanced-k8s-config"
        serverUrl: "https://avanced-k8s-config:443"
        skipTlsVerify: true
        namespace: "default"
        credentialsId: "advanced-k8s-credentials"
        jenkinsUrl: "http://jenkins/"
        connectTimeout: 0
        readTimeout: 0
        containerCapStr: 100
        maxRequestsPerHostStr: 64
        retentionTimeout: 5
        templates:
          - name: "k8s-slave"
            namespace: "default"
            label: "linux-x86_64"
            nodeUsageMode: EXCLUSIVE
            containers:
              - name: "jnlp"
                image: "jenkinsci/jnlp-slave:latest"
                alwaysPullImage: true
                workingDir: "/home/jenkins"
                ttyEnabled: true
                resourceRequestCpu: "500m"
                resourceLimitCpu: "1000m"
                resourceRequestMemory: "1Gi"
                resourceLimitMemory: "2Gi"
            volumes:
              - emptyDirVolume:
                  memory: false
                  mountPath: "/tmp"
            idleMinutes: "1"
            activeDeadlineSeconds: "120"
            slaveConnectTimeout: "1000"
```