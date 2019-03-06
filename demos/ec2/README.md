# configure Amazon EC2 plugin

https://wiki.jenkins.io/display/JENKINS/Amazon+EC2+Plugin

## sample configuration

```yaml
jenkins:
  clouds:
  - amazonEC2:
      cloudName: "docker-agents"
      credentialsId: "jenkins-aws"
      privateKey: "${EC2_PRIVATE_KEY}"
      region: "eu-central-1"
      templates:
      - ami: "ami-xyz"
        amiType:
          unixData:
            sshPort: "22"
        associatePublicIp: false
        connectBySSHProcess: false
        connectUsingPublicIp: false
        deleteRootOnTermination: false
        description: "docker"
        ebsOptimized: false
        idleTerminationMinutes: "10"
        labelString: "docker ubuntu linux"
        mode: NORMAL
        monitoring: false
        numExecutors: 1
        remoteAdmin: "ubuntu"
        securityGroups: "docker"
        stopOnTerminate: false
        type: T2Micro
        useDedicatedTenancy: false
        useEphemeralDevices: false
        usePrivateDnsName: false
      useInstanceProfileForCredentials: false
```
