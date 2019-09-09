# role-strategy-plugin

Basic configuration of the [Role-based Authorization Strategy plugin](https://plugins.jenkins.io/role-strategy)

Requires `role-strategy` >= 2.11

## sample

```yml
jenkins:
  authorizationStrategy:
    roleBased:
      roles:
        global:
          - name: "admin"
            description: "Jenkins administrators"
            permissions:
              - "Overall/Administer"
            assignments:
              - "admin"
          - name: "readonly"
            description: "Read-only users"
            permissions:
              - "Overall/Read"
              - "Job/Read"
            assignments:
              - "authenticated"
        items:
          - name: "FolderA"
            description: "Jobs in Folder A, but not the folder itself"
            pattern: "A/.*"
            permissions:
              - "Job/Configure"
              - "Job/Build"
              - "Job/Delete"
            assignments:
              - "user1"
              - "user2"
          - name: "FolderB"
            description: "Jobs in Folder B, but not the folder itself"
            pattern: "B.*"
            permissions:
              - "Job/Configure"
              - "Job/Build"
            assignments:
              - "user2"
        agents:
          - name: "Agent1"
            description: "Agent 1"
            pattern: "agent1"
            permissions:
              - "Agent/Build"
            assignments:
              - "user1"

  securityRealm:
    local:
      allowsSignup: false
      users:
        - id: "admin"
          password: "1234"
        - id: "user1"
          password: ""
        - id: "user_hashed"
          # password is password
          password: "#jbcrypt:$2a$10$3bnAsorIxhl9kTYvNHa2hOJQwPzwT4bv9Vs.9KdXkh9ySANjJKm5u"

  nodes:
    - dumb:
        mode: NORMAL
        name: "agent1"
        remoteFS: "/home/user1"
        launcher: jnlp
    - dumb:
        mode: NORMAL
        name: "agent2"
        remoteFS: "/home/user1"
        launcher: jnlp
```

which is taken from the plugins' [integration test resources](../../integrations/src/test/resources/io/jenkins/plugins/casc/RoleStrategy1.yml)
