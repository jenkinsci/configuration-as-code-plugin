package jenkins.benchmark.jmh;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Runs sample benchmarks from JUnit tests.
 */
public class CascJmhBenchmarkStateTest {
    @Test
    public void testJmhBenchmarks() throws Exception {
        // create directory for JMH reports
        Path path = Paths.get("target/jmh-reports/");
        Files.createDirectories(path);

        // number of iterations is kept to a minimum just to verify that the benchmarks work without spending extra
        // time during builds.
        ChainedOptionsBuilder optionsBuilder =
                new OptionsBuilder()
                        .forks(1)
                        .warmupIterations(1)
                        .warmupBatchSize(1)
                        .measurementIterations(1)
                        .measurementBatchSize(1)
                        .shouldFailOnError(true)
                        .result("target/jmh-reports/jmh-benchmark-report.json")
                        .timeUnit(TimeUnit.MICROSECONDS)
                        .resultFormat(ResultFormatType.JSON);
        BenchmarkFinder finder = new BenchmarkFinder(this.getClass().getPackage().getName());
        finder.findBenchmarks(optionsBuilder);
        new Runner(optionsBuilder.build()).run();
    }
}
