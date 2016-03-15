# Installing plugin
To require that a plugin and all its dependencies be present, regardless of its version,
```
plugin 'ldap'
```

To require that a plugin of at least the following version be present,
```
plugin 'ldap', '1.5'
```

This will trigger an installation of a plugin if the current version is older than that.

Due to the current limitation in update center, it is not possible to require that
a specific version of a plugin be present. When an installation is triggered, the latest
version advertised from the update center gets installed.

Currently, the best way to avoid this limitation is to manually lay down `*.jpi` files
by using Chef/Puppet/etc.