# build agents

Build agents configuration belongs under `jenkins` root element

## Sample configuration

```yaml
jenkins:
  nodes:
    - permanent:
        labelString: "linux docker test"
        mode: NORMAL
        name: "utility-node"
        remoteFS: "/home/user1"
        launcher:
          inbound:
            webSocket: true
            tunnel: some.proxy
            workDirSettings:
              disabled: true
              failIfWorkDirIsMissing: false
              internalDir: "remoting2"
              workDirPath: "/tmp"

    - permanent:
        labelString: "linux docker test"
        mode: NORMAL
        name: "utility-node-2"
        numExecutors: 4
        remoteFS: "/home/user2"
        launcher:
          ssh:
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

## node-specific labels

In addition to `labelString`, permanent nodes support a `labels` property that accepts a flat or nested array of
label strings. Both can be used together: `labelString` values are applied first, then all values from `labels` are
appended, producing a single combined label string on the agent.

This is especially useful when multiple nodes share common platform labels via a YAML anchor while each
node declares its own tool-specific labels through `labels`.

- `labelString` — supported for backward compatibility; stays unchanged on export
- `labels` — new array-based property for node-specific labels
- The resulting label string is `labelString` values followed by `labels` values, space-separated

```yaml
x-node-defaults: &node-defaults
  remoteFS: "/tmp"
  launcher: "jnlp"
  labelString: "linux docker"

jenkins:
  nodes:
    - permanent:
        name: "java-node"
        <<: *node-defaults
        labels:
          - "java"
          - "maven"

    - permanent:
        name: "python-node"
        <<: *node-defaults
        labels:
          - "python"
          - "pytest"
```

With this configuration `java-node` will have the combined label string `linux docker java maven` and `python-node`
will have `linux docker python pytest`.
