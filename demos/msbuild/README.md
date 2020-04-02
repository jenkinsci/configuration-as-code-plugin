# Configure MSBuild

Basic configuration of [MSBuild](https://plugins.jenkins.io/msbuild) plugin.

## Sample configuration

```yaml
tool:
  msbuild:
    installations:
      - name: "MSBuild Latest"
        home: "C:\\WINDOWS\\Microsoft.NET\\Framework\\14.0\\Bin\\MSBuild.exe"
        defaultArgs: "/p:Configuration=Debug"
```
