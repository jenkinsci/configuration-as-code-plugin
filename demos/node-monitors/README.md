# Configuring Node Monitors

Node monitor configuration belongs under `jenkins` root element.
Requires at least Jenkins `2.433`.<br/>
Requires at least version TODO of [Versions Node Monitors](https://plugins.jenkins.io/versioncolumn/) plugin

Any monitor that is available but is not configured will be treated as ignored.
Some monitors are capable to take agents offline when they are not ignored. Ignored
monitors will still run and report data.


```yaml
jenkins:
  nodeMonitors:
    - "architecture"
    - "clock"
    - diskSpace:
        freeSpaceThreshold: "3GB"
    - "swapSpace"
    - tmpSpace:
        freeSpaceThreshold: "3GB"
    - responseTime:
        ignored: true
    - jvmVersion:  # from Versions Node Monitors plugin
        comparisonMode: EXACT_MATCH
        disconnect: true
    - "remotingVersion"  # from Versions Node Monitors plugin 
    - inodesMonitor:  # from inodes-monitor plugin
        inodesPercentThreshold: "98%"
```