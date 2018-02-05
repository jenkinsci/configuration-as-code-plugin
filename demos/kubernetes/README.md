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
