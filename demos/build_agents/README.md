# build agents

Build agents configuration belongs (currently) under `jenkins` root element

## sample configuration

```yaml
jenkins:
  (...)
  nodes:
    - dumb:
        labelString: "linux docker test"
        mode: NORMAL
        name: "utility-node"
        remoteFS: "/home/user1"
        launcher:
          jnlp:
    - dumb:
        labelString: "linux docker test"
        mode: NORMAL
        name: "utility-node-2"
        remoteFS: "/home/user2"
        launcher:
          SSHLauncher:
            host: "192.168.1.1"
            port: 22
            credentialsId: test
            launchTimeoutSeconds: 60
            maxNumRetries: 3
            retryWaitTime: 30
            sshHostKeyVerificationStrategy:
              manuallyTrustedKeyVerificationStrategy:
                requireInitialManualTrust: false
```
