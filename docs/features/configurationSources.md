# Configuration Sources

JCasC loads configuration from one or more locations specified by the
`CASC_JENKINS_CONFIG` environment variable or the
`casc.jenkins.config` Java system property.

## Built-in Sources

JCasC includes fetchers for:

- Local files and directories
- `file://` URIs
- `http://` URLs
- `https://` URLs

When a directory is specified, JCasC recursively discovers YAML files.

## Authenticated Sources

Configuration fetchers may use credentials supplied through registered
`FetchCredentialsProvider` extensions.

When a configuration source is requested, JCasC resolves available
credentials and passes them to the selected `CasCConfigFetcher`.

This allows extension plugins to support authenticated configuration
sources such as private Git repositories, cloud object storage, or
other protected services.

### Using Authenticated Sources

To load configuration from an authenticated source, install or use a
`CasCConfigFetcher` implementation that supports your source (for example, 
a source supported by an installed fetcher plugin) and configure
the corresponding `FetchCredentialsProvider`.

When JCasC loads the configuration, it automatically resolves available
credentials and passes them to the fetcher responsible for the configured
source.

## Extending Support

Plugins can add support for additional configuration sources by
implementing `CasCConfigFetcher`.

Plugins can provide credentials for those sources by implementing
`FetchCredentialsProvider`.