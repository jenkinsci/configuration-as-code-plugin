# warnings-plugin

Supported in plugin **version >= 4.66**

## sample-configuration (Example parser from help)

```yaml
jenkins: 
  [...]
unclassified:
  warnings:
    parsers:
      - name: "Example parser"
        linkName: "Example parser link"
        trendName: "Example parser trend name"
        regexp: "^\\s*(.*):(\\d+):(.*):\\s*(.*)$"
        script: |
          import hudson.plugins.warnings.parser.Warning
          String fileName = matcher.group(1)
          String lineNumber = matcher.group(2)
          String category = matcher.group(3)
          String message = matcher.group(4)
          return new Warning(fileName, Integer.parseInt(lineNumber), "Dynamic Parser", category, message);
        example: "somefile.txt:2:SeriousWarnings:SomethingWentWrong"
```



