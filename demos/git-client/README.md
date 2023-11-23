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

## More detailed tool configuration

```yaml
tool:
  git:
    installations:
    - home: "git"
      name: "Default"
      properties:
      - installSource:
          installers:
          - command:
              command: "true"
              label: "!windows && !freebsd && !openbsd"
              toolHome: "/usr/bin/git"
          - command:
              command: "true"
              label: "freebsd || openbsd"
              toolHome: "/usr/local/bin/git"
          - batchFile:
              command: "exit 0"
              label: "windows"
              toolHome: "C:\\tools\\MinGit-2.42.0.2\\mingw64\\bin\\git.exe"
```

## Sample ssh host key verification policy configuration

The [ssh host key verification policy](https://plugins.jenkins.io/git-client/#plugin-content-ssh-host-key-verification) can be configured with configuration as code.
Other ssh host key verification policy examples are available in the [git client plugin documentation](https://plugins.jenkins.io/git-client/#plugin-content-configuration-as-code-sample).

```yaml
security:
  gitHostKeyVerificationConfiguration:
    sshHostKeyVerificationStrategy:
      manuallyProvidedKeyVerificationStrategy:
        approvedHostKeys: |-
          bitbucket.org ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIIazEu89wgQZ4bqs3d63QSMzYVa0MuJ2e2gKTKqu+UUO
          github.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOMqqnkVzrm0SdG6UOoqKLsabgH5C9okWi0dh2l9GKJl
          gitlab.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIAfuCHKVTjquxvt6CM6tdG4SLp1Btn/nOeHHE5UOzRdf
```
