# configure mesos plugin

## sample configuration

```yaml
jenkins:
  clouds:
    - mesos:
        checkpoint: false
        declineOfferDuration: 600
        description: "My Mesos Cloud"
        frameworkName: "Jenkins Framework"
        jenkinsURL: "https://jenkins.mesos.cloud"
        master: 1.2.3.4:8000
        nativeLibraryPath: "/usr/lib/libmesos.so"
        onDemandRegistration: true
        role: "*"
        slavesUser: "jenkins"
        slaveInfos:
          - node01:
            labelString: 'docker'
            slaveCpus: 0.1
            slaveMem: 512
            slaveAttributes: >
                  {"rack":"jenkins-build-agents"}
            executorCpus: 0.1
            executorMem: 128
            diskNeeded: 0.0
            minExecutors: 1
            maxExecutors: 2
            remoteFSRoot: 'jenkins'
            jvmArgs: ''
            jnlpArgs: ''
            defaultSlave: true
            idleTerminationMinutes: '5'
            containerInfo:
              type: "docker"
              dockerImage: "cloudbees/java-with-docker-client:latest"
              networking: "BRIDGE"
              volumes:
                - containerPath: "/var/run/docker.sock"
                  hostPath: "/var/run/docker.sock"
                  readOnly: "false"
                - containerPath: "/tmp/jenkins/workspace/"
                  hostPath: "/tmp/jenkins/workspace/"
                  readOnly: "false"
```

## implementation note

Jenkins singleton doesn't offer any `setClouds` method. So here we rely on a pseudo-property implemented by a dedicated
`Attribute` to add the configured clouds to `Jenkins.clouds`. The current implementation only adds the configured cloud
if it doesn't exists yet.
