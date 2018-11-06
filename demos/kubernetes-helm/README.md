# Install the Plugin using the Official Jenkins helm chart

## Preparation

Jenkins can be installed in Kubernetes using [`helm`](https://github.com/helm/helm).
The latest stable helm chart can be found [`here`](https://github.com/helm/charts/tree/master/stable/jenkins).

Now grab a copy of the helm chart [`values file`](https://github.com/helm/charts/blob/master/stable/jenkins/values.yaml) and adjust the Master part a little bit:

```yaml
Master:
  Name: jenkins-master
  Image: "jenkins/jenkins"
  ImageTag: "2.134-alpine"
  ImagePullPolicy: "IfNotPresent"
  Component: "jenkins-master"
  UseSecurity: true
  AdminUser: admin
  AdminPassword: "formetoknowforyoutofindout"
  Cpu: "200m"
  Memory: "1024Mi"

  InitContainerEnv:

  ContainerEnv:
    # Tell the plugin where to find its config. The '..data'
    # part is needed for now due to this bug:
    # https://github.com/jenkinsci/configuration-as-code-plugin/issues/425
    - name: CASC_JENKINS_CONFIG
      value: /var/jenkins_home/casc_configs/..data/jenkins.yaml
    # `SECRETS` is used to override the `/run/secrets` path,
    # which is useful for setting credentials.
    # But it needs to be used in conjuction with Master.SecretsFilesSecret
    # - name: SECRETS
    #   value: /usr/share/jenkins/ref/secrets

  # List of plugins to be install during Jenkins master start
  # mind the last plugin in the list now ;)
  InstallPlugins:
    - kubernetes:latest
    - kubernetes-credentials:latest
    - workflow-aggregator:latest
    - workflow-job:latest
    - credentials-binding:latest
    - git:latest
    - configuration-as-code:1.0
    # You might also need this for a couple of third-party plugins (e.g. for setting credentials)
    - configuration-as-code-support:1.0

Persistence:
  volumes:
  - name: casc-config
    configMap:
      name: jenkins-casc-config
  mounts:
  - name: casc-config
    mountPath: /var/jenkins_home/casc_configs
    readOnly: true
```

You will also need to create a Kubernetes ConfigMap that will contain your JCASC config file. Create a new file (below we'll assume you named it `jenkins-casc-config.yaml`) with the following contents:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: jenkins-casc-config
data:
  jenkins.yaml: |
    jenkins:
      systemMessage: "CASC Jenkins is cool"
```

## Installation

First, upload your ConfigMap to Kubernetes:

```
kubectl create -f jenkins-casc-config.yaml
```

Now, deploy the Helm chart with those customized values:

```
helm install --name jenkins stable/jenkins -f values.yaml
```

Once Helm finishes deploying the chart, connect to your Jenkins server in your browser. You should see the `CASC Jenkins is cool` system message displayed at the top. Congratulations, you've installed the plugin and made your first Jenkins configuration change successfully! :)

## Misc

The Jenkins Helm chart can be extended to make use of a custom ConfigMap. If you are already using this facility for other reasons, you can also include your custom JCASC config in that ConfigMap (instead of creating a new ConfigMap outside of the Helm chart as the above installation guide instructs).

Check the [`custom ConfigMap`](https://github.com/helm/charts/tree/master/stable/jenkins#custom-configmap) section in the Jenkins Helm chart repo for more information.
