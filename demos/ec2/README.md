# Configure Amazon EC2 plugin

Basic configuration of the [Amazon EC2 Plugin](https://plugins.jenkins.io/ec2)

## sample configuration

```yaml
x-ec2_anchor: &ec2_anchor
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
  tenancy: Default
  useEphemeralDevices: false
  zone: "us-east-1"
  ami: "ami-0c6bb742864ffa3f3"
  securityGroups: "some-group"
  remoteFS: "/home/ec2-user"
  remoteAdmin: "ec2-user"
  mode: "NORMAL"
  amiType:
    unixData:
      rootCommandPrefix: "sudo"
      slaveCommandPrefix: "sudo -u jenkins"
      sshPort: "61120"

jenkins:
  clouds:
    - amazonEC2:
        cloudName: "ec2"
        instanceCapStr: 20
        # this shouldn't be needed, since without explicit creds this should already be used
        # but let's be explicit to avoid issues.
        useInstanceProfileForCredentials: true
        # Reminder: the following key has multiple lines
        sshKeysCredentialsId: "ssh-key-credential-id"
        noDelayProvisioning: true
        region: "eu-central-1"
        templates:
          - description: "Auto configured EC2 Agent Small"
            type: "T2Small"
            labelString: "Small"
            <<: *ec2_anchor
          - description: "Auto configured EC2 Agent Large"
            type: "T2Xlarge"
            labelString: "Large"
            <<: *ec2_anchor
```
