# Configure Amazon EC2 plugin

Basic configuration of the [Amazon EC2 Plugin](https://plugins.jenkins.io/ec2)

## sample configuration

```yaml

jenkins:
  clouds:
    - amazonEC2:
        cloudName: "ec2"
        instanceCapStr: 20
        # this shouldn't be needed, since without explicit creds this should already be used
        # but let's be explicit to avoid issues.
        useInstanceProfileForCredentials: true
        # Reminder: the following key has multiple lines
        privateKey: "${EC2_PRIVATE_KEY}"
        noDelayProvisioning: true
        region: "eu-central-1"
        templates:
          - description: "Auto configured EC2 Agent, yay again"
            associatePublicIp: false
            connectBySSHProcess: false
            connectionStrategy: PRIVATE_IP
            deleteRootOnTermination: false
            ebsOptimized: false
            idleTerminationMinutes: "10"
            maxTotalUses: -1
            monitoring: false
            numExecutors: 1
            stopOnTerminate: false
            useDedicatedTenancy: false
            useEphemeralDevices: false
            zone: "us-east-1"
            ami: "ami-0c6bb742864ffa3f3"
            labelString: "test yey"
            type: "T2Xlarge"
            securityGroups: "some-group"
            remoteFS: "/home/ec2-user"
            remoteAdmin: "ec2-user"
            mode: "NORMAL"
            amiType:
              unixData:
                rootCommandPrefix: "sudo"
                slaveCommandPrefix: "sudo -u jenkins"
                sshPort: "61120"
```
