# Configure View Job Filters

Basic configuration of the [View Job Filters plugin](https://plugins.jenkins.io/view-job-filters/)

## Sample configuration

```yaml
jenkins:
  views:
    - list:
        columns:
        - "status"
        - "weather"
        - "jobName"
        - "lastSuccess"
        - "lastFailure"
        - "lastDuration"
        - "buildButton"
        jobFilters:
        - buildDurationFilter:
            amount: "60.0"
            amountTypeString: "Days"
            buildCountTypeString: "Latest"
            buildDurationMinutes: "5"
            includeExcludeTypeString: "includeMatched"
            lessThan: true
        - buildStatusFilter:
            building: false
            inBuildQueue: true
            includeExcludeTypeString: "includeMatched"
            neverBuilt: true
        - securityFilter:
            build: false
            configure: true
            includeExcludeTypeString: "includeMatched"
            permissionCheckType: "MustMatchAll"
            workspace: false
        name: "MyFirstView"
```
