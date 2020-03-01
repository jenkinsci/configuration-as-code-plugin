package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.jmh.CascJmhBenchmarkState;
import javax.annotation.Nonnull;
import jenkins.benchmark.jmh.JmhBenchmark;
import jenkins.model.Jenkins;
import org.openjdk.jmh.annotations.Benchmark;

import static org.junit.Assert.assertEquals;

@JmhBenchmark
public class SampleBenchmark {
    public static class MyState extends CascJmhBenchmarkState {
        @Nonnull
        @Override
        protected String getResourcePath() {
            return "benchmarks.yml";
        }

        @Nonnull
        @Override
        protected Class<?> getEnclosingClass() {
            return SampleBenchmark.class;
        }
    }

    @Benchmark
    public void benchmark(MyState state) {
        Jenkins jenkins = state.getJenkins();
        assertEquals("Benchmark started with Configuration as Code", jenkins.getSystemMessage());
        assertEquals(22, jenkins.getNumExecutors());
    }
}
