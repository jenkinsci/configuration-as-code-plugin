# configure installed plugins

Configuration-as-Code can manage plugin installation, assuming Configuration-as-Code-plugin is
initially installed (chicken & egg ... )!  

YAML configuration can declare a set of required plugins and version. We require
a version as the primary goal is to ensure reproducibility, but we also support `latest`
as version number for your convenience.

For plugins not hosted on an update center, you can set a download URL as version.

## sample configuration

```yaml
plugins:
  required:
    git: 3.6
    chucknorris: latest
    my-custom-plugin: http://download.acme.com/my-custom-plugin-1.0.jpi
```

## implementation note

Plugin installation with a specific version requires an update center to expose
metadata per hosted plugin version, not just latest. 

Jenkins community update center exposes [`plugin-versions.json`](https://updates.jenkins.io/current/plugin-versions.json)
metadata file. Proprietary update centers might not, in such case you will have
to use download URLs in your YAML file.
