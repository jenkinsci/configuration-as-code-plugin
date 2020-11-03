# Install the plugin using the official Jenkins helm chart

## Preparation

Jenkins can be installed in Kubernetes using [helm](https://github.com/helm/helm).
The latest stable helm chart can be found [here](https://github.com/jenkinsci/helm-charts).

Now grab a copy of the helm chart [values file](https://github.com/jenkinsci/helm-charts/blob/main/charts/jenkins/values.yaml) and adjust the Master part a little bit:

```yaml
master:
  componentName: jenkins-controller
  image: 'jenkins/jenkins'
  tag: 'lts'
  imagePullPolicy: 'Always'
  useSecurity: true
  adminUser: admin
  adminPassword: 'formetoknowforyoutofindout'
  resources:
    requests:
      cpu: '50m'
      memory: '256Mi'
    limits:
      cpu: '2000m'
      memory: '4096Mi'

  # Below is the implementation of Jenkins Configuration as Code.  Add a key under ConfigScripts for each configuration area,
  # where each corresponds to a plugin or section of the UI.  Each key (prior to | character) is just a label, and can be any value.
  # Keys are only used to give the section a meaningful name.  The only restriction is they may only contain RFC 1123 \ DNS label
  # characters: lowercase letters, numbers, and hyphens.  The keys become the name of a configuration YAML file on the controller in
  # /var/jenkins_home/casc_configs (by default) and will be processed by the Configuration as Code plugin.  The lines after each |
  # become the content of the configuration YAML file.  The first line after this is a JCasC root element, eg jenkins, credentials,
  # etc.  Best reference is https://<jenkins_url>/configuration-as-code/reference.  The example below creates a welcome message:
  JCasC:
    enabled: true
    pluginVersion: 1.35
    configScripts:
      welcome-message: |
        jenkins:
          systemMessage: Welcome to our CI\CD server.  This Jenkins is configured and managed 'as code'.

  sidecars:
    configAutoReload:
      # If enabled: true, Jenkins Configuration as Code will be reloaded on-the-fly without a reboot.  If false or not-specified,
      # JCasC changes will cause a reboot and will only be applied at the subsequent start-up.  Auto-reload uses the Jenkins CLI
      # over SSH to reapply config when changes to the ConfigScripts are detected.  The admin user (or account you specify in
      # Master.AdminUser) will have a random SSH private key (RSA 4096) assigned unless you specify OwnSshKey: true.  This will be saved to a k8s secret.
      enabled: true
      image: shadwell/k8s-sidecar:0.0.2
      imagePullPolicy: IfNotPresent
      resources:
        #   limits:
        #     cpu: 100m
        #     memory: 100Mi
        #   requests:
        #     cpu: 50m
        #     memory: 50Mi
      # SSH port value can be set to any unused TCP port.  The default, 1044, is a non-standard SSH port that has been chosen at random.
      # Is only used to reload JCasC config from the sidecar container running in the Jenkins controller pod.
      # This TCP port will not be open in the pod (unless you specifically configure this), so Jenkins will not be
      # accessible via SSH from outside of the pod.  Note if you use non-root pod privileges (RunAsUser & FsGroup),
      # this must be > 1024:
      sshTcpPort: 1044
      # label that the configmaps with Configuration as Code config are marked with:
      label: jenkins_config
      # folder in the pod that should hold the collected Configuration as Code config:
      folder: /var/jenkins_home/casc_configs
      # If specified, the sidecar will search for config-maps inside this namespace.
      # Otherwise the namespace in which the sidecar is running will be used.
      # It's also possible to specify ALL to search in all namespaces:
      # searchNamespace:

  initContainerEnv:

  containerEnv:
    # Tell the plugin where to find its config.
    - name: CASC_JENKINS_CONFIG
      value: /var/jenkins_home/casc_configs/jenkins.yaml
    # `SECRETS` is used to override the `/run/secrets` path,
    # which is useful for setting credentials.
    # But it needs to be used in conjunction with Master.SecretsFilesSecret
    # - name: SECRETS
    #   value: /usr/share/jenkins/ref/secrets

  # List of plugins to be install during Jenkins controller start
  # mind the last plugin in the list now ;)
  installPlugins:
    - kubernetes:latest
    - kubernetes-credentials:latest
    - workflow-aggregator:latest
    - workflow-job:latest
    - credentials-binding:latest
    - git:latest
```

Now, deploy the Helm chart with those customized values:

```bash
helm install jenkins stable/jenkins -f values.yaml
```

Once Helm finishes deploying the chart, connect to your Jenkins server in your browser. You should see the welcome system message displayed at the top. Congratulations, you've installed the plugin and made your first Jenkins configuration change successfully! :)

## Misc

Check the [Configuration as Code](https://github.com/jenkinsci/helm-charts/tree/main/charts/jenkins#configuration-as-code) section in the Jenkins Helm chart repo for more information.
