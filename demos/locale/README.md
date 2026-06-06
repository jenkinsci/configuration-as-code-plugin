# Locale Plugin

This demo shows how to configure the Jenkins UI language using the [Locale Plugin](https://plugins.jenkins.io/locale/) via Jenkins Configuration as Code.

This plugin is commonly used to set the Jenkins UI language globally and optionally ignore the browser's language preferences.

## Configuration

The Locale configuration is located under the `appearance` root element.

```yaml
appearance:
  locale:
    systemLocale: "en"
    ignoreAcceptLanguage: true