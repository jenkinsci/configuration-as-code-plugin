# Usage scenarios

This sums up a few usage scenarios for Jenkins configuration as code and our recommendations how to use it.

It's based on some real customer cases and potential users we already have plans with or could see using it in early stages of the project 2018.

In the early stage of the project, we use the scenarios to detect the most important missing features. Later we will use the scenarios as documentation and guidance on how to adapt the configuration as code approach.


## Manage and version control Jenkins global configuration

A small group of devops engineers are maintaining the company's Jenkins installations. There are a handful of Jenkins master's, an a few hundred slaves.
In general the devops team have full access to all the relevant infrastructure and are responsible for the documentation, compliance, maintenance and continued operation.

From their IT department they are supplied with base server or client installations for servers and clients, and they apply configuration management practices to maintain the infrastructure.

For their Jenkins masters the only configuration not under version control is the Jenkins global configuration. The devops engineers maintain that, together with a set of trusted lead developers.

Their global configuration is most frequently changes related to (in order of most changed first): slave configuration, Jenkins plug-in updates, Jenkins global configuration changes.
All their Jenkins master have common pieces in common (e.g. Artifactory plugin configuration or mail setup) and some instance specific configuration because not all masters are used for the same kind of software development so plug-ins differs.

In general the devops team agree that the running instances serves as documentation of the configuration themselves - one can always look in the UI. And they can restore from backup.
Their problems are always related to changes, where they update a plug-in or change a global configuration that gives unexpected side-effects and is rather hard to debug for the users or devops team members that do not know about it.
They try to remember to ping each other on their chat to inform about changes.

The devops team would like more traceability around changes, by completely move to manage the Jenkins global configuration as code and put it under version control. They will like to benefit also from having it as code to be able to spin up test environments and re-use common configuration across instances.


**With Jenkins Configuration as Code plug-in** installed on their master, the devops team will describe all the configuration in the `jenkins.yml` and maintain it through a git repository. When-ever they want to change anything, they will edit the file in git and their production server will refresh the configuration or the list of installed plugi-ns.

From that point on they all have full traceability of all the changes made, and they are able to roll it back easily by reverting commits. They can review the changes simply by looking at the git diffs on commits.

**Getting there and adopting Jenkins Configuration as Code** is also quite easy for the devops team. They will need to install the plug-in, and go the the Configuration as Code menu in Manage Jenkins, and use the export function to create the initial YAML files that represent their current global configuration and plug-in's installed. Export is not intended to offer a directly usable jenkins.yaml configuration. It can be used for inspiration writting your own, but be aware that export can be partial, or fail for some components.

When the files are in git, they can point to the file in the repository in the Configuration as Code menu, and click reload so Jenkins updates it's configuration accordingly.


_Migration step-by-step guide:_ For the exact steps, see this part of the migration guide - TBD when all relevant issues fixed:

  * [#6 about reloading configuration when changed](https://github.com/jenkinsci/configuration-as-code-plugin/issues/6)
  * [#7 about plug-in installation support](https://github.com/jenkinsci/configuration-as-code-plugin/issues/7)
  * [#10 related to hierarchy and re-use of configuration](https://github.com/jenkinsci/configuration-as-code-plugin/issues/10)
  * [#32 where to draft a migration to Jenkins Configuration as Code guide](https://github.com/jenkinsci/configuration-as-code-plugin/issues/32)
  * [#65 helping users migrate by export existing configuration](https://github.com/jenkinsci/configuration-as-code-plugin/issues/65)



## Jenkins master already in docker

A small full stack developer team have their single Jenkins master running in a container and they orchestrate and deploy it using one of the many container orchestration tools.
Their Jenkins global configuration is the only piece not under version control, so they depend on the current data in `JENKINS_HOME` to persist the Jenkins global configuration and be able to redeploy the Jenkins master with existing installed plug-ins. They do not care about their historic build data.

**With Jenkins configuration as code plug-in** they can get their missing configuration under version control as well and their installed plug-in combinations and for this little full stack developer team it means they can always just redeploy their Jenkins should something be wrong or failing, and their container orchestration tool will keep their Jenkins master always running for them completely automatically.

**Migrating to use the Jenkins Configuration as Code plug-in** is easy if they can live with doing just few manual steps before it is all automated. They need to install the Jenkins Configuration as Code plug-in on their current master, then use the export functionality to create the `plugins.yml` and `jenkins.yml` file for them, which they need to add to a git repository.
Then the last thing they need to do is to update their deployment recipe to use the customized Jenkins where the Jenkins Configuration as Code plug-in comes pre-installed, and have the recipe pass the URI to the `jenkins.yml` file when starting Jenkins.

After that they only do changes in the configuration file through git, and Jenkins Configuration as Code will refresh global configuration. Should their infrastructure fail their orchestration tool will recreate their master from scratch, but still with the same configuration and without depending on old data sets.

Notice a slight difference between `plugins.yml` as part of the Configuration as Code vs `plugins.txt` known from the Jenkins container setups. The latter one will need to contain all plugins and their dependencies and will usually be auto generated to support starting a Jenkins container with all the plug-ins one like. The `plugin.yml` takes the approach of only mentioning the plugin and their version (or latest) the user really cares about, and Configuration as Code will resolve dependencies for the user.

This means the team above will only have to maintain the `plugins.yml` entries for plugins they use and then make sure their clean Jenkins they start have the Configuration as Code plug-in installed. That can be done with a customized container image or the one supplied with Configuration as Code plugin.


_Migration step-by-step guide:_ For the exact steps, see this part of the migration guide - TBD when all relevant issues fixed:

  * [#6 about reloading configuration when changed](https://github.com/jenkinsci/configuration-as-code-plugin/issues/6)
  * [#7 about plug-in installation support](https://github.com/jenkinsci/configuration-as-code-plugin/issues/7)
  * [#32 where to draft a migration to Jenkins Configuration as Code guide](https://github.com/jenkinsci/configuration-as-code-plugin/issues/32)
  * [#65 helping users migrate by export existing configuration](https://github.com/jenkinsci/configuration-as-code-plugin/issues/65)
  * [#70 explaining we need the Docker image the team above need to use](https://github.com/jenkinsci/configuration-as-code-plugin/issues/70)
