# Configure External Workspace Manager

Sample configuration of the [External Workspace Manager plugin](https://plugins.jenkins.io/external-workspace-manager)

## sample configuration

```yaml
jenkins:
  systemMessage: "Jenkins configured automatically by Jenkins Configuration as Code Plugin"
  numExecutors: 5
  scmCheckoutRetryCount: 2
  mode: NORMAL
  nodeProperties:
    - exwsNodeConfigurationDiskPools:
        nodeDiskPools:
          - diskPoolRefId: "controller-node-id"
            nodeDisks:
              - diskRefId: "controller-node-disk"
                nodeMountPoint: "/tmp/controller-node"

  nodes:
    - permanent:
        name: "static-agent"
        remoteFS: "/home/jenkins"
        nodeProperties:
          - exwsNodeConfigurationDiskPools:
              nodeDiskPools:
                - diskPoolRefId: "localhostdiskpool"
                  nodeDisks:
                    - diskRefId: "localdisk"
                      nodeMountPoint: "/tmp/localdisk"
  slaveAgentPort: 50000
  agentProtocols:
    - "jnlp2"

unclassified:
  exwsGlobalConfigurationDiskPools:
    diskPools:
      - diskPoolId: "diskpool1"
        disks:
          - diskId: "disk1"
            displayName: "disk one display name"
            diskInfo: "noDiskInfo"
            masterMountPoint: "/tmp"
        displayName: "diskpool1 display name"
        restriction: "any"
        strategy: "mostUsableSpace"
  exwsGlobalConfigurationTemplates:
    templates:
      - label: "first"
        nodeDiskPools:
          - diskPoolRefId: "dp1"
            nodeDisks:
              - diskRefId: "dp1refid1"
                nodeMountPoint: "/tmp/template11"
              - diskRefId: "dp1refid2"
                nodeMountPoint: "/tmp/template12"
          - diskPoolRefId: "dp2"
            nodeDisks:
              - diskRefId: "dp2refid1"
                nodeMountPoint: "/tmp/template21"
      - label: "second"
        nodeDiskPools:
          - diskPoolRefId: "dp3"
            nodeDisks:
              - diskRefId: "dp3refid1"
                nodeMountPoint: "/tmp/template31"
              - diskRefId: "dp3refid2"
                nodeMountPoint: "/tmp/template32"
          - diskPoolRefId: "dp4"
            nodeDisks:
              - diskRefId: "dp4refid1"
                nodeMountPoint: "/tmp/template41"
      - label: "third"
        nodeDiskPools:
          - diskPoolRefId: "dp5"
            nodeDisks:
              - diskRefId: "dp5refid1"
                nodeMountPoint: "/tmp/template51"
              - diskRefId: "dp5refid2"
                nodeMountPoint: "/tmp/template52"
          - diskPoolRefId: "dp6"
            nodeDisks:
              - diskRefId: "dp6refid1"
                nodeMountPoint: "/tmp/template61"
      - label: "fourth"
        nodeDiskPools:
          - diskPoolRefId: "dp7"
            nodeDisks:
              - diskRefId: "dp7refid1"
                nodeMountPoint: "/tmp/template71"
              - diskRefId: "dp7refid2"
                nodeMountPoint: "/tmp/template72"
          - diskPoolRefId: "dp8"
            nodeDisks:
              - diskRefId: "dp8refid1"
                nodeMountPoint: "/tmp/template81"
      - label: "fifth"
        nodeDiskPools:
          - diskPoolRefId: "dp9"
            nodeDisks:
              - diskRefId: "dp9refid1"
                nodeMountPoint: "/tmp/template91"
              - diskRefId: "dp9refid2"
                nodeMountPoint: "/tmp/template92"
          - diskPoolRefId: "dp10"
            nodeDisks:
              - diskRefId: "dp10refid1"
                nodeMountPoint: "/tmp/template101"
              - diskRefId: "dp10refid2"
                nodeMountPoint: "/tmp/template102"
              - diskRefId: "dp10refid3"
                nodeMountPoint: "/tmp/template103"
              - diskRefId: "dp10refid4"
                nodeMountPoint: "/tmp/template104"
```
