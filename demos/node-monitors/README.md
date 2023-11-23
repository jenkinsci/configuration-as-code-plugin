# Configuring Node Monitors

Node monitor configuration belongs under `jenkins` root element.
Requires at least Jenkins `2.433`.<br/>

Any monitor that is available but is not configured will be treated as ignored.
Some monitors are capable to take agents offline when they are not ignored. Ignored
monitors will still run and report data.

## sample configuration

```yaml
jenkins:
  nodeMonitors:
    - "architecture"
    - diskSpace:
        freeSpaceThreshold: "3GB"
    - "swapSpace"
    - tmpSpace:
        freeSpaceThreshold: "3GB"
    - responseTime:
        ignored: true
```