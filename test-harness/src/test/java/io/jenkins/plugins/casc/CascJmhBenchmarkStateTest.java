package io.jenkins.plugins.casc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import hudson.Functions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import jenkins.benchmark.jmh.BenchmarkFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Runs sample benchmarks from JUnit tests.
 */
class CascJmhBenchmarkStateTest {
    private static final String reportPath = "target/jmh-reports/jmh-benchmark-report.json";

    @BeforeEach
    void createDirectoryForBenchmarkReport() throws IOException {
        Path directory = Paths.get("target/jmh-reports/");
        Files.createDirectories(directory);
    }

    @Test
    void testJmhBenchmarks() throws Exception {
        assumeFalse(
                Functions.isWindows() && System.getenv("CI") != null,
                "TODO Cannot run program C:\\openjdk-11\\bin\\java.exe: CreateProcess error=206, The filename or extension is too long");
        // number of iterations is kept to a minimum just to verify that the benchmarks work without spending extra
        // time during builds.
        ChainedOptionsBuilder optionsBuilder = new OptionsBuilder()
                .forks(1)
                .warmupIterations(0)
                .measurementBatchSize(1)
                .measurementIterations(1)
                .shouldFailOnError(true)
                .result(reportPath)
                .timeUnit(TimeUnit.MICROSECONDS)
                .resultFormat(ResultFormatType.JSON);
        new BenchmarkFinder(getClass()).findBenchmarks(optionsBuilder);
        new Runner(optionsBuilder.build()).run();

        assertTrue(Files.exists(Paths.get(reportPath)));
    }
}
