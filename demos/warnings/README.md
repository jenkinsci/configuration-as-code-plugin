# Configure Warnings Next Generation Plugin

The available parsers of the [Warnings Next Generation Plugin](https://plugins.jenkins.io/warnings-ng)
can be specified using the following sample configuration. Afterwards,
these parsers are shown in the `Groovy Based Warnings Parsers` section of the system configuration.

## Sample configuration (parsers)

Required plugin version: 5.0.0 or newer.

```yaml
unclassified:
  warningsParsers:
    parsers:
      - name: "Example parser"
        id: example-id
        regexp: "^\\s*(.*):(\\d+):(.*):\\s*(.*)$"
        script: |
          import edu.hm.hafner.analysis.Severity
          builder.setFileName(matcher.group(1))
                  .setLineStart(Integer.parseInt(matcher.group(2)))
                  .setSeverity(Severity.WARNING_NORMAL)
                  .setCategory(matcher.group(3))
                  .setMessage(matcher.group(4))
          return builder.buildOptional();
        example: "somefile.txt:2:SeriousWarnings:SomethingWentWrong"
```

This `Example Parser` parser will parse the following warning from the console log:
```text
somefile.txt:2:SeriousWarnings:SomethingWentWrong
```

It will produce a warning with the following properties:

| property    | value              |
|-------------|--------------------|
| file name   | somefile.txt       |
| line number | 2                  |
| severity    | NORMAL             |
| category    | SeriousWarnings    |
| type        | -                  |
| message     | SomethingWentWrong |

See [documentation](https://github.com/jenkinsci/warnings-ng-plugin/blob/master/doc/Documentation.md) of the
Warnings Next Generation Plugin for more details about the parsers.
