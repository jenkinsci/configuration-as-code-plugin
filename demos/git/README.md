# configure git plugin

(actually, git-client plugin)

## sample configuration

```yaml
tool:
  git:
    installations:
      - name: git
        home: /usr/local/bin/git
```

## implementation note

Here we rely on `hudson.tools.ToolDescriptor.setInstallations`, so same applies to all ToolInstallations.
Unfortunately Java reflection makes it hack-ish to detect the parameter type of this method from derived concrete
class, so maybe there's some corner case we will need to polish this logic.

Also, YAML lists are converted into `ArrayLists` but `setInstallations(T ... installation)` varags method require
an array - blame Java to not just accept any `Iterable` - so we need to detect this scenario and do the type
conversion.
