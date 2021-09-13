package io.jenkins.plugins.casc;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jenkins.plugins.casc.misc.jmh.CascJmhBenchmarkState;
import jenkins.benchmark.jmh.JmhBenchmark;
import jenkins.model.Jenkins;
import org.openjdk.jmh.annotations.Benchmark;

import static org.junit.Assert.assertEquals;

@JmhBenchmark
public class SampleBenchmark {
    public static class MyState extends CascJmhBenchmarkState {
        @NonNull
        @Override
        protected String getResourcePath() {
            return "benchmarks.yml";
        }

        @NonNull
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
