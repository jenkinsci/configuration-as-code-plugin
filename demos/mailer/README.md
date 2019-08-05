# mailer plugin

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

~~Descriptor defines setters so we can inject configuration, but for SMTP authentication parameters.
See [jenknisci/mailer-plugin#39](https://github.com/jenkinsci/mailer-plugin/pull/39)~~

The PR [jenknisci/mailer-plugin#39](https://github.com/jenkinsci/mailer-plugin/pull/39) to properly fix setting the smtp-server is stil open (and under review?), yet a different PR [jenknisci/mailer-plugin#42](https://github.com/jenkinsci/mailer-plugin/pull/42) "fixed" the inability to set the smtp-server in the mean time.
So as long as a [mailer-plugin](https://github.com/jenkinsci/mailer-plugin) version newer than 1.22 is installed seting the smtp-server via JCasC works.
