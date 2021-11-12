# Configure MSBuild

Basic configuration of [MSTestRunner](https://plugins.jenkins.io/mstestrunner/) plugin.

## Sample configuration

```yaml
tool:
  msTestInstallation:
    installations:
    - defaultArgs: "/category:SmokeTests"
      home: "C:\\Program Files (x86)\\Microsoft Visual Studio 10.0\\Common7\\IDE\\MSTest.exe"
      name: "MSTest test"
      omitNoIsolation: true
```
