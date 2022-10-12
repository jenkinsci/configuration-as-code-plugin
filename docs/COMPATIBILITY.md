# Plugin Compatibility issues with Configuration as Code plugin

Often JCasC is not the one having issues with configuring a plugin.
Because of how the JCasC was designed, plugins _should_ be expected to work out of the box if they followed the main patterns in place.
We rely heavily on data bindings so if a plugin is not setup according to expected design pattern for data binding.

JCasC cannot communicate properly with the plugin without fixing their data binding issues or writing a special configurator.
The path of least resistance is fixing the data binding, since other plugins in the Jenkins eco-system rely heavily on data binding.

The list of already known issues in the Jenkins issue tracker, are tracked through using the label `jcasc-compatibility` [see dashboard][dashboard].

## Reporting Plugin Compatibility issue

Create an issue at [issues.jenkins.io](https://issues.jenkins.io) with the label `jcasc-compatibility` and fill out the details.
[Link to create issue][new-jira-issue]
Create a Github issue for crosslink purposes on this repository.

If you prefer the [new issue for plugin compatibility][new-github-issue] should take you through the entire workflow.

[dashboard]: https://issues.jenkins.io/secure/Dashboard.jspa?selectPageId=18341
[new-jira-issue]: https://issues.jenkins.io/secure/CreateIssueDetails!init.jspa?pid=10172&issuetype=1&summary=Cannot+configure+X+plugin+with+JCasC&labels=jcasc-compatibility
[new-github-issue]: https://github.com/jenkinsci/configuration-as-code-plugin/issues/new?assignees=&labels=plugin-compatibility&template=4-plugin-compatibility.yml
