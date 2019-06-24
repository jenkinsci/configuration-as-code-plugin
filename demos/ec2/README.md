# configure Amazon EC2 plugin

[Amazon EC2 Plugin](https://wiki.jenkins.io/display/JENKINS/Amazon+EC2+Plugin)

## sample configuration

```yaml
jenkins:
  clouds:
    - amazonEC2:
        cloudName: "docker-agents"
        credentialsId: "jenkins-aws"
        privateKey: "${EC2_PRIVATE_KEY}"
        region: "eu-central-1"
        noDelayProvisioning: true
        useInstanceProfileForCredentials: false
        templates:
          - ami: "ami-xyz"
            amiType:
              unixData:
                sshPort: "22"
            associatePublicIp: false
            connectBySSHProcess: false
            connectionStrategy: PRIVATE_IP
            deleteRootOnTermination: false
            description: "docker"
            ebsOptimized: false
            idleTerminationMinutes: "10"
            labelString: "docker ubuntu linux"
            maxTotalUses: -1
            mode: NORMAL
            monitoring: false
            numExecutors: 1
            remoteAdmin: "ubuntu"
            securityGroups: "docker"
            stopOnTerminate: false
            type: T2Micro
            useDedicatedTenancy: false
            useEphemeralDevices: false
```
