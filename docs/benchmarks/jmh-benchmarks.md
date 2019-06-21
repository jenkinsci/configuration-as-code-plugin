# JMH benchmarks with Configuration-as-Code

You can configure the instance that is started for benchmarks using Configuration as Code by extending
`CascJmhBenchmarkState` in your benchmarks instead of `JmhBenchmarkState` and overriding the `getResourcePath()` and
`getEnclosingClass()` methods.

`getResourcePath()` should returning the path to where your YAML configuration is located.
`getEnclosingClass()` should return the class containing the state for the benchmark (`MyBenchmark` in the example below).

## Example

Just like regular JMH benchmarks using `JmhBenchmarkState`, you need to have a `public static` inner class:

```java
@JmhBenchmark
public class MyBenchmark {
    public static class MyState extends CascJmhBenchmarkState {
        @Nonnull
        @Override
        protected String getResourcePath() {
            return "config.yaml";
        }
    
        @Nonnull
        @Override
        protected Class<?> getEnclosingClass() {
            return MyBenchmark.class;
        }
    }
    
    // ...
}
```

If you override the `setup()` method of `CascJmhBenchmarkState`, make sure to call `super.setup()` so 
that configuration as code works as intended.

You can find more examples in the [Role Strategy Plugin](https://github.com/jenkinsci/role-strategy-plugin/tree/master/src/test/java/jmh).
