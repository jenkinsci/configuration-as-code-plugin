
# Demo Examples

## Conventions

* Each folder maps to a single demo example.
  * The folder `git` shows how to configure git with Configuration as Code
* If a demo example folder is named after a plugin, then the demo this plugin is automatically install
  * The Github Demo Example folder is named `github`, wich corresponds to the plugin `github.hpi`

* [Mandatory] Documentation of the example, conveying the intent: `README.md`
* [Mandatory] Configuration as Code Snippet of the example: `config.yaml`
* [Mandatory] Maven POM Descriptor: `pom.xml`
* [Optional] Addon to the default Docker Compose stack: `custom-compose.yml`
* [Optional] Replacement of the default Docker Compose stack: `overwrite-compose.yml`
* [Optional] Adds more plugin: `plugins.txt`
* [Optional] Custom Integration Tests: `tests/*.bats`
