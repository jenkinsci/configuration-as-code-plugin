# Configure Locale plugin

Basic configuration of the [Locale plugin](https://plugins.jenkins.io/locale)

The Locale plugin allows administrators to control the language settings of the Jenkins user interface, overriding browser preferences and setting a default language for all users.

## sample configuration

```yaml
appearance:
  locale:
    systemLocale: en
    ignoreAcceptLanguage: true
    allowUserPreferences: false
```

### Configuration options

- `systemLocale`: The default language code to use for the Jenkins UI (e.g., `en`, `fr`, `de`, `ja`, `zh_CN`, `de_AT`)
- `ignoreAcceptLanguage`: If `true`, ignores browser language preferences and forces all users to use the system locale
- `allowUserPreferences`: If `true`, allows users to set their own language preferences. If `false`, users must use the system locale

### Example: Force English for all users

This configuration forces all users to use English, ignoring browser preferences and not allowing user preferences:

```yaml
appearance:
  locale:
    systemLocale: en
    ignoreAcceptLanguage: true
    allowUserPreferences: false
```

### Example: Default to French but allow user preferences

This configuration sets French as the default but allows users to override it:

```yaml
appearance:
  locale:
    systemLocale: fr
    ignoreAcceptLanguage: false
    allowUserPreferences: true
```

### Example: Default to German (Austria) with browser preference fallback

This configuration sets Austrian German as default but respects browser preferences if not forced:

```yaml
appearance:
  locale:
    systemLocale: de_AT
    ignoreAcceptLanguage: false
    allowUserPreferences: true
```

