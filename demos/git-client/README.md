# Configure Git client plugin

Basic configuration of a git tool to be used by the [Git client plugin](https://plugins.jenkins.io/git-client)

## Sample tool configuration

```yaml
tool:
  git:
    installations:
      - name: git
        home: /bin/git
      - name: another_git
        home: /usr/local/bin/git
```
