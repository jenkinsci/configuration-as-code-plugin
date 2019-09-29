# Configure mercurial

Basic configuration of the [Mercurial](https://plugins.jenkins.io/mercurial)

## sample configuration

```yaml
tool:
  mercurialinstallation:
    installations:
      - name: "Mercurial 3"
        home: "/mercurial"
        config: |
          [defaults]
          clone = --uncompressed
          bundle = --type none
        executable: "INSTALLATION/bin/hg"
        useCaches: true
        debug: false
        masterCacheRoot: "/cache/root"
        useSharing: false
        properties:
          - installSource:
              installers:
                - command:
                    toolHome: "mercurial"
                    label: "SomeLabel"
                    command: "[ -d mercurial ] || wget -q -O - http://www.archlinux.org/packages/extra/x86_64/mercurial/download/ | xzcat | tar xvf -"
```
