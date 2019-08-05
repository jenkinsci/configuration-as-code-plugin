# mailer plugin

Requires `mailer-plugin` >= 1.22

Mailer plugin configuration belongs under `unclassified` root element

## sample configuration

```yaml
unclassified:
  mailer:
    adminAddress: admin@acme.org
    replyToAddress: do-not-reply@acme.org
    smtpHost: smtp.acme.org
    smtpPort: 4441
    charset: UTF-8
    useSsl: false
```

## implementation note

`hudson.task.Mailer.Descriptor` exposes global SMTP configuration parameters.
It is identified as YAML root element `mailer` as this descriptor has a `global.jelly` UI view, so JCasC
assumes it makes sense to expose it as a root element extension.
