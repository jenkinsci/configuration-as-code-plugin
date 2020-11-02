# Configure docker plugin

Basic configuration of the [Docker plugin](https://plugins.jenkins.io/docker-plugin)

For plugin version 1.1.2 and up:

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
              # TODO: pin sha256 or versions when using in production
              image: "jenkins/agent"
              volumes:
                - hello:/hello
                - world:/world
              environmentsString: |
                hello=world
                foo=bar
            remoteFs: "/home/jenkins/agent"
            connector:
              attach:
                user: "jenkins"
            instanceCapStr: "10"
            retentionStrategy:
              idleMinutes: 1
```

## implementation note

Jenkins singleton doesn't offer any `setClouds` method. So here we rely on a pseudo-property implemented by a dedicated
`Attribute` to add the configured clouds to `Jenkins.clouds`. The current implementation only adds the configured cloud
if it doesn't exists yet.
