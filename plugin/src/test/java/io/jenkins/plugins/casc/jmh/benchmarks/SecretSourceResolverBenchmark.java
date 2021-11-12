package io.jenkins.plugins.casc.jmh.benchmarks;

import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import jenkins.benchmark.jmh.JmhBenchmark;
import jenkins.benchmark.jmh.JmhBenchmarkState;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;

@JmhBenchmark
@BenchmarkMode(Mode.Throughput)
public class SecretSourceResolverBenchmark {
    public static class JenkinsState extends JmhBenchmarkState {
        private final EnvironmentVariables environmentVariables = new EnvironmentVariables();
        private ConfigurationContext context = null;

        @Override
        public void setup() {
            ConfiguratorRegistry registry = ConfiguratorRegistry.get();
            context = new ConfigurationContext(registry);
            environmentVariables.set("FOO", "HELLO");
            environmentVariables.set("BAR", "WROLD");
        }
    }

    @Benchmark
    public void emptyString(JenkinsState state, Blackhole blackhole) {
        blackhole.consume(state.context.getSecretSourceResolver().resolve(""));
    }

    @Benchmark
    public void textButNoSecret(JenkinsState state, Blackhole blackhole) {
        blackhole.consume(state.context.getSecretSourceResolver().resolve("HELLO:WORLD"));
    }

    @Benchmark
    public void singleSecret(JenkinsState state, Blackhole blackhole) {
        blackhole.consume(state.context.getSecretSourceResolver().resolve("${FOO}"));
    }

    @Benchmark
    public void multipleSecrets(JenkinsState state, Blackhole blackhole) {
        blackhole.consume(state.context.getSecretSourceResolver().resolve("${FOO}:${BAR}"));
    }
}
