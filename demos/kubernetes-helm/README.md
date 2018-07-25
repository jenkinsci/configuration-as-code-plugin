# Install the Plugin using the Official Jenkins helm chart

## Preparation

Jenkins can be installed in Kubernetes using [`helm`](https://github.com/helm/helm).
The latest stable helm chart can be found [`here`](https://github.com/helm/charts/tree/master/stable/jenkins).

To have the latest Version of JCASC installed we need to enable Jenkins to make use of the [`Experimental Update Center`](https://jenkins.io/doc/developer/publishing/releasing-experimental-updates/).

The [`official docker image`](https://github.com/jenkinsci/docker/blob/master/Dockerfile#L60) provides enabling the Experimental Update Center via ENV vars:  

```
ENV JENKINS_UC https://updates.jenkins.io
ENV JENKINS_UC_EXPERIMENTAL=https://updates.jenkins.io/experimental
ENV JENKINS_INCREMENTALS_REPO_MIRROR=https://repo.jenkins-ci.org/incrementals
```

Now grab a copy of the helm chart [`values file`](https://github.com/helm/charts/blob/master/stable/jenkins/values.yaml) and adjust the Master part a little bit:

```
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
  ContainerEnv:
    - name: JENKINS_UC
      value: https://updates.jenkins.io
    - name: JENKINS_UC_EXPERIMENTAL
      value: https://updates.jenkins.io/experimental
    - name: JENKINS_INCREMENTALS_REPO_MIRROR
      value: https://repo.jenkins-ci.org/incrementals
  InitContainerEnv:
    - name: JENKINS_UC
      value: https://updates.jenkins.io
    - name: JENKINS_UC_EXPERIMENTAL
      value: https://updates.jenkins.io/experimental
    - name: JENKINS_INCREMENTALS_REPO_MIRROR
      value: https://repo.jenkins-ci.org/incrementals

  # List of plugins to be install during Jenkins master start
  # mind the last plugin in the list now ;)
  InstallPlugins:
      - kubernetes:latest
      - kubernetes-credentials:latest
      - workflow-aggregator:latest
      - workflow-job:latest
      - credentials-binding:latest
      - git:latest
      - configuration-as-code:0.10-alpha       
...      
```

## Installations

Now just deploy the helm chart with those customized values:

```
helm install --name jenkins stable/jenkins \
-f values.yml
```

Watch the init-container creating the stuff:

```
kubectl logs -f jenkins-{POD_NAME} -c copy-default-config
```

You should see something like:

```
Creating initial locks...
Analyzing war...
Registering preinstalled plugins...
Downloading plugins...
Downloading plugin: kubernetes from https://updates.jenkins.io/download/plugins/kubernetes/latest/kubernetes.hpi
Downloading plugin: kubernetes-credentials from https://updates.jenkins.io/download/plugins/kubernetes-credentials/latest/kubernetes-credentials.hpi
...
Installed plugins:
ace-editor:1.1
apache-httpcomponents-client-4-api:4.5.5-3.0
authentication-tokens:1.3
cloudbees-folder:6.5.1
config-file-provider:2.18
configuration-as-code:0.10-alpha
credentials-binding:1.16
credentials:2.1.18
display-url-api:2.2.0
docker-commons:1.13
docker-workflow:1.17
durable-task:1.22
...
```

Done :)

## Next?

You can extend the Jenkins Helm Chart to make use of a Custom Config map.
This would enable you to mount in some additional files.

Check [`this`](https://github.com/helm/charts/tree/master/stable/jenkins#custom-configmap) link on how to do so.
