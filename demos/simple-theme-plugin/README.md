# Configure simple-theme-plugin plugin

Basic configuration of the [Simple Theme plugin](https://plugins.jenkins.io/simple-theme-plugin)

## sample configuration

```yaml
appearance:
  simpleTheme:
    elements:
      - cssUrl:
          url: "https://example.bogus/test.css"
      - cssText:
          text: ".testcss { color: red }"
      - jsUrl:
          url: "https://example.bogus/test.js"
      - faviconUrl:
          url: "https://vignette.wikia.nocookie.net/deadpool/images/6/64/Favicon.ico"
```
