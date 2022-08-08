# Configure git plugin

Basic configuration of the [Git Client plugin](https://plugins.jenkins.io/git-client)

## sample configuration

```yaml
tool:
  git:
    installations:
      - name: git
        home: /bin/git
      - name: another_git
        home: /usr/local/bin/git
security:
  gitHostKeyVerificationConfiguration:
    sshHostKeyVerificationStrategy:
      manuallyProvidedKeyVerificationStrategy:
        approvedHostKeys: |-
          github.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOMqqnkVzrm0SdG6UOoqKLsabgH5C9okWi0dh2l9GKJl
          gitlab.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIAfuCHKVTjquxvt6CM6tdG4SLp1Btn/nOeHHE5UOzRdf
```

## implementation note

Here we rely on `hudson.tools.ToolDescriptor.setInstallations`, so same applies to all ToolInstallations.
Unfortunately Java reflection makes it hack-ish to detect the parameter type of this method from derived concrete
class, so maybe there's some corner case we will need to polish this logic.

Also, YAML lists are converted into `ArrayLists` but `setInstallations(T ... installation)` varags method require
an array - blame Java to not just accept any `Iterable` - so we need to detect this scenario and do the type
conversion.
