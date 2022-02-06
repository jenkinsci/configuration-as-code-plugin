# Configure pipeline-maven

Configuration examples for the [Pipeline Maven plugin](https://plugins.jenkins.io/pipeline-maven)

This part only concerns the Maven integration into Jenkins pipelines. To configure Maven itself, refer to [its own examples](../maven/README.md)

## Default configuration

This is the default configuration of the plugin, doing this or nothing is equal

```yaml
tool:
  pipelineMaven:
    triggerDownstreamUponResultSuccess: true
```

## Configure triggers

Pipeline Maven plugin can [trigger downstream pipelines](https://github.com/jenkinsci/pipeline-maven-plugin#feature-trigger-downstream).

By default, this is only done on a successfull build, but is configurable. For example, if you want triggering on unstable too:

```yaml
tool:
  pipelineMaven:
    triggerDownstreamUponResultAborted: false
    triggerDownstreamUponResultFailure: false
    triggerDownstreamUponResultNotBuilt: false
    triggerDownstreamUponResultSuccess: true
    triggerDownstreamUponResultUnstable: true
    triggerDownstreamUponResultSuccess: false
```

## Configure database

To compute dependencies triggering, the plugin [has a database](https://github.com/jenkinsci/pipeline-maven-plugin#db-setup).
By default, an in-memory H2 one. For production use, it is recommended to configure a PostgreSQL or a MySQL database.

### MySQL

```yaml
credentials:
  system:
    domainCredentials:
    - credentials:
      - usernamePassword:
          description: "MySQL database credentials"
          id: "my-creds"
          password: "${MYSQL_PASSWORD}"
          scope: GLOBAL
          username: "jenkinsuser"

tool:
  pipelineMaven:
    jdbcUrl: jdbc:mysql://dbserver/jenkinsdb
    JdbcCredentialsId: my-creds
```

### PostgreSQL

```yaml
credentials:
  system:
    domainCredentials:
    - credentials:
      - usernamePassword:
          description: "Postgres database credentials"
          id: "pg-creds"
          password: "${POSTGRESQL_PASSWORD}"
          scope: GLOBAL
          username: "jenkinsuser"

tool:
  pipelineMaven:
    jdbcUrl: jdbc:postgresql://dbserver/jenkinsdb
    JdbcCredentialsId: pg-creds
```

### Custom properties

If needed, custom connection properties can be specified:

```yaml
tool:
  pipelineMaven:
    jdbcUrl: "jdbc:mysql://dbserver/jenkinsdb"
    properties: |
      dataSource.cachePrepStmts=true
      dataSource.prepStmtCacheSize=250
```

## Publishers

You can also configure publishers, to disable some of them, or to specify default behaviours:

```yaml
tool:
  pipelineMaven:
    publisherOptions:
    - dependenciesFingerprintPublisher:
        includeReleaseVersions: true
        includeScopeProvided: false
        includeScopeRuntime: false
        includeSnapshotVersions: false
    - findbugsPublisher:
        disabled: true
    - openTasksPublisher:
        disabled: true
    - pipelineGraphPublisher:
        lifecycleThreshold: "package"
    - spotbugsPublisher:
        disabled: true
```
