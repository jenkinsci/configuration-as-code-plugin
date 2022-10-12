# Configure Kubernetes plugin

Jenkins can be installed in Kubernetes and preconfigured to run jobs (and other
options) in the Kubernetes cluster, using YAML stored in a `ConfigMap`.
See [config.yml](config.yml) for the `ConfigMap` definition.

## Advanced sample configuration

```yaml
unclassified:
  location:
    url: http://jenkins/

jenkins:
  clouds:
    - kubernetes:
        name: "advanced-k8s-config"
        serverUrl: "https://avanced-k8s-config:443"
        serverCertificate: "serverCertificate"
        skipTlsVerify: true
        credentialsId: "advanced-k8s-credentials"
        namespace: "default"
        jenkinsUrl: "http://jenkins/"
        jenkinsTunnel: "jenkinsTunnel"
        containerCapStr: 42
        maxRequestsPerHostStr: 64
        retentionTimeout: 5
        connectTimeout: 10
        readTimeout: 20

        templates:
          - name: "test"
            serviceAccount: "serviceAccount"
            instanceCap: 1234
            idleMinutes: 0
            label: "label"
            # Enable whether the POD Yaml is displayed in each build log or not, `true` by default.
            showRawYaml: true
            
            volumes:
              - hostPathVolume:
                  mountPath: "mountPath"
                  hostPath: "hostPath"

            containers:
              - name: "name"
                image: "image"
                privileged: true
                alwaysPullImage: true
                command: "command"
                args: "args"
                workingDir: "workingDir"
                ttyEnabled: true
                resourceRequestCpu: "resourceRequestCpu"
                resourceRequestMemory: "resourceRequestMemory"
                resourceLimitCpu: "resourceLimitCpu"
                resourceLimitMemory: "resourceLimitMemory"
            imagePullSecrets:
              - name: "imagePullSecrets"

            envVars:
              - envVar:
                  key: "FOO"
                  value: "BAR"

          - name: "k8s-agent"
            namespace: "default"
            label: "linux-x86_64"
            nodeUsageMode: EXCLUSIVE
            containers:
              - name: "jnlp"
                image: "jenkins/inbound-agent:latest"
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
              # Mount the content of the ConfigMap `configmap-name` with the data `config`.
              - configMapVolume:
                  configMapName: configmap-name
                  mountPath: /home/jenkins/.aws/config
                  subPath: config
            idleMinutes: "1"
            activeDeadlineSeconds: "120"
            slaveConnectTimeout: "1000"
```

## Example installation on Kubernetes

```bash
kubectl apply -f service-account.yml
kubectl apply -f config.yml
kubectl apply -f jenkins.yml
```
