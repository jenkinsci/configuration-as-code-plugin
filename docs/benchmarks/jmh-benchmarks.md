# JMH benchmarks with Configuration-as-Code

You can configure the instance that is started for benchmarks using Configuration as Code by extending
`CascJmhBenchmarkState` in your benchmarks instead of `JmhBenchmarkState` and overriding the `getResourcePath()` method and returning the path to where your YAML configuration is located.

## Example

Just like regular JMH benchmarks using `JmhBenchmarkState`, you need to have a `public static` inner class:

```java
public static class MyState extends CascJmhBenchmarkState {
    @Nonnull
    @Override
    protected String getResourcePath() {
        return "path/to/config.yaml";
    }
}
```

If you override the `setup()` method of `CascJmhBenchmarkState`, make sure to call `super.setup()` so 
that configuration as code works as intended.

You can find more examples in the [Role Strategy Plugin](https://github.com/jenkinsci/role-strategy-plugin/tree/master/src/test/java/jmh/casc).
