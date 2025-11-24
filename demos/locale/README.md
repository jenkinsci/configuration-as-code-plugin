# Configure Locale Plugin

## Sample Configuration

Sample configuration for the [Locale plugin](https://plugins.jenkins.io/locale).

This plugin allows you to set Jenkins UI language and ignore browser language preferences.

### Minimal Configuration

Set Jenkins UI to English:

```yaml
appearance:
  locale:
    systemLocale: "en"
```

### Full Configuration

Set Jenkins UI to English and ignore browser Accept-Language header:

```yaml
appearance:
  locale:
    systemLocale: "en"
    ignoreAcceptLanguage: true
```

## Options

- `systemLocale`: The locale to use (e.g., "en", "en_US", "de", "fr", "ja", "zh_CN")
- `ignoreAcceptLanguage`: If true, ignore browser's Accept-Language header and always use systemLocale

## Notes

- Without `ignoreAcceptLanguage: true`, Jenkins will still respect user's browser language settings
- This is useful for teams that want consistent UI language regardless of browser settings
- Supported locales depend on installed language packs

