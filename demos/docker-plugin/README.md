# Docker Plugin — SSH Connector JCasC Demo

This demo provides a working Jenkins Configuration as Code (JCasC) example for configuring
the Docker plugin with the SSH connector. The existing documentation does not show how to
set up SSH-based agents using the docker-plugin, so this example is added to fill that gap.

## SSH connector usage

The docker-plugin exposes the `sshConnector` field for configuring SSH-based cloud agents.
Using other keys such as `ssh:` does not match the expected structure and will cause a
configuration error.

A working configuration can be found in `docker-plugin-ssh.yaml`. The key part of the
connector configuration is:

```yaml
connector:
  sshConnector:
    sshKeyCredentialsId: "my-ssh-key-id"
    javaPath: "/opt/java/openjdk/bin/java"
