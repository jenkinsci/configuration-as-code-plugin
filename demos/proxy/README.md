# Configure proxy

## Sample configuration

```yaml
jenkins:
  proxy:
    name: "proxyhost"
    port: 80
    userName: "login"
    password: "password"
    noProxyHost: "externalhost"
    testUrl: "http://google.com"
```