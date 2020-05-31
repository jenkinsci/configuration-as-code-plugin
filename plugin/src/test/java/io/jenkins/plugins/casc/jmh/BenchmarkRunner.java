package io.jenkins.plugins.casc.jmh;

import java.util.concurrent.TimeUnit;
import jenkins.benchmark.jmh.BenchmarkFinder;
import org.junit.Test;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public final class BenchmarkRunner {
    @Test
    public void runJmhBenchmarks() throws Exception {
        ChainedOptionsBuilder options = new OptionsBuilder()
            .mode(Mode.AverageTime)
            .warmupIterations(2)
            .timeUnit(TimeUnit.MICROSECONDS)
            .threads(2)
            .forks(2)
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .resultFormat(ResultFormatType.JSON)
            .result("jmh-report.json");

        BenchmarkFinder bf = new BenchmarkFinder(getClass());
        bf.findBenchmarks(options);
        new Runner(options.build()).run();
    }
}
