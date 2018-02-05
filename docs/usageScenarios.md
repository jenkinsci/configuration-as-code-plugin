# Usage scenarios

This sums up a few usage scenarios for Jenkins configuration as code and our recommendations how to use it.

It's based on some real customer cases and potential users we already have plans with or could see using it in early stages of the project 2018.


## Manage and version control Jenkins global configuration

A small group of devops engineers are maintaining the company's Jenkins installations. There are a handful of Jenkins master's, an a few hundred slaves.
In general the devops team have full access to all the relevant infrastructure and are responsible for the documentation, compliance, maintenance and continued operation.

From their IT department they are supplied with base server or client installations for servers and clients, and they apply configuration management practices to maintain the infrastructure.

For their Jenkins masters the only configuration not under version control is the Jenkins global configuration. The devops engineers maintain that, together with a set of trusted lead developers.

Their global configuration is most frequently changes related to (in order of most changed first): slave configuration, Jenkins plug-in updates, Jenkins global configuration changes.
All their Jenkins master have common pieces in common (e.g. Artifactory plugin configuration or mail setup) and some instance specific configuration because not all masters are used for the same kind of software development so plug-ins differs.

In general the devops team agree that the running instances serves as documentation of the configuration themselves - one can always look in the UI. And they can restore from backup.
Their problems are always related to changes, where they update a plugin or change a global configuration that gives unexpected side-effects and is rather hard to debug for the users or devops team members that do not know about it.
They try to remember to ping each other on their chat to inform about changes.

The devops team would like more traceability around changes, by completely move to manage the Jenkins global configuration as code and put it under version control. They will like to benefit also from having it as code to be able to spin up test environments and re-use common configuration across instances.


**With Jenkins configuration as code plugin** installed on their master, the devops team will describe all the configuration in the `jenkins.yml` and maintain it through a git repository. When-ever they want to change anything, they will edit the file in git and their production server will refresh the configuration or the list of installed plugins.

From that point on they all have full traceability of all the changes made, and they are able to roll it back easily by reverting commits. They can review the changes simply by looking at the git diffs on commits.

**Getting there and adopting Jenkins configuration as code** is also quite easy for the devops team. They will need to install the plugin, and go the the Configuration as Code menu in Manage Jenkins, and use the export function to create the initial YAML files that represent their current global configuration and plugin's installed.

When the files are in git, they can point to the file in the repository in the Configuration as Code menu, and click reload so Jenkins updates it's configuration accordingly.







## Infrastructure as code Jenkins master in docker

TBD the company already using docker, they just need to version control the configuration

## Migrating existing instance

TBD ... export functionality not yet there... but basically install plugin and run export and save the file, then ...
