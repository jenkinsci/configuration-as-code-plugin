# Configure xvfb tool installation

Basic configuration of the [xvfb](https://plugins.jenkins.io/xvfb/)

## sample configuration

For plugin version 1.1:

```yaml
tool:
  xvfbInstallation:
    installations:
      - name: "default"
```

For plugin version 1.2 and up:

```yaml
tool:
  xvfb:
    installations:
      - name: "default"
        home: ""
```
