# configure docker plugin

## sample configuration

```yaml
jenkins:
  clouds:
    - docker:
        name: "docker"
        dockerApi:
          dockerHost:
            uri: "unix:///var/run/docker.sock"
        templates:
          - labelString: "docker-agent"
            dockerTemplateBase:
              image: "jenkins/slave"
            remoteFs: "/home/jenkins/agent"
            connector:
              attach:
                user: "jenkins"
            instanceCapStr: "10"
```

## implementation note

Jenkins singleton doesn't offer any `setClouds` method. So here we rely on a pseudo-property implemented by a dedicated 
`Attribute` to add the configured clouds to `Jenkins.clouds`. The current implementation only adds the configured cloud 
if it doesn't exists yet. 
 
