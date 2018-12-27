# Run Groovy scripts

Configuration-as-Code can run Groovy scripts.

Groovy scripts cen be:
 * inline, with entry `script`
 * from `url`
 * from local file, using `file`

## Sample configuration

```yaml
groovy:
  - script: >
      println("This is Groovy script!");
```



## Implementation notes

 * It is recommended to use semicolons at the end of lines
 * There is no dry run implemented for Groovy scripts feature

