# Configure GoLang

Basic configuration of [Golang](https://plugins.jenkins.io/golang/)

## Sample configuration

```yaml
tool:
  go:
    installations:
      - name: "go_lang"
        properties:
          - installSource:
              installers:
                - golangInstaller:
                    id: "1.18.3"
```
