# mailer plugin

Mailer plugin configuration belongs under `unclassified` root element

## sample configuration

```yaml
jenkins:
  [...]
unclassified:
  mailer:
    adminAddress: admin@acme.org
    replyToAddress: do-not-reply@acme.org
    # Note that this does not work right now
    #smtpHost: smtp.acme.org
    smtpPort: 4441
    charset: UTF-8
    useSsl: false
```

## implementation note

`hudson.task.Mailer.Descriptor` do expose global SMTP configuration parameters.
It is identified as yaml root element `mailer` as this descriptor has a `global.jelly` UI view, so configuration-as-code
assumes it make sense to expose it as a root element extension.

Descriptor do define setters so we can inject configuration, but for SMTP authentication parameters.
See https://github.com/jenkinsci/configuration-as-code-plugin/issues/2.
See https://github.com/jenkinsci/mailer-plugin/pull/39
