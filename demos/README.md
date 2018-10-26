
# Demo Examples

## Conventions

* Each folder with a name not starting by a dot is considered as a single demo example:
  * The folder `git/` is a demo example, showing how to configure Git with Configuration as Code.
  * The folder `.commons/` is not a demo example.
* If the name of a given demo example folder is named after a plugin, then the demo this plugin is automatically installed:
  * The folder named `github`, which shows how to configure GitHub with Configuration as Code, corresponds to the plugin `github.hpi`: it will be automatically installed by the testing tool.
  * The folder named `jenkins` does not map to any plugin: no plugin will be installed automatically.

* Each demo example folder **MUST** have the following files:
  * `README.md`: Documentation of the example, conveying the intent.
  * `config.yaml`: YAML Snippet of the Configuration as Code example.

* Each demo example folder **MAY** have the following files (not mandatory):
  * `plugins.txt`: describes the set of additional plugins to be installed for this example, as described in [Preinstallaing Plugin for Jenkins Docker Image](https://github.com/jenkinsci/docker#preinstalling-plugins). Useful to specify the right plugin when it does not follow the convention below, as `gitlab` -> `gitlab-plugin.hpi`.
  * `custom-compose.yml`: describes additional docker-compose services to run with the Jenkins used for testing.
  * `overwrite-compose.yml`: used instead of the default `docker-compose.yml`.
  * `tests/*.bats`: describe additional test suites to run for this demo example, using [Bats Testing Framework](https://github.com/sstephenson/bats).
