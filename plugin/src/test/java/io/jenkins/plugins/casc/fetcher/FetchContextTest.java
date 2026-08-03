package io.jenkins.plugins.casc.fetcher;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

public class FetchContextTest {

    @Test
    public void testDeterministicSorting() {
        ResolvedYaml yamlB = new ResolvedYaml("z-last.yaml", () -> new ByteArrayInputStream(new byte[0]));
        ResolvedYaml yamlA = new ResolvedYaml("a-first.yaml", () -> new ByteArrayInputStream(new byte[0]));

        FetchResult result = new FetchResult(Arrays.asList(yamlB, yamlA), (AutoCloseable) null);

        try (FetchContext context = new FetchContext()) {
            context.add(result);

            String firstSource = context.getSources().get(0).toString();
            String secondSource = context.getSources().get(1).toString();

            assertTrue("a-first.yaml should be sorted first", firstSource.contains("a-first.yaml"));
            assertTrue("z-last.yaml should be sorted last", secondSource.contains("z-last.yaml"));
        }
    }

    @Test
    public void testContextClosesAllResults() {
        AtomicBoolean result1Closed = new AtomicBoolean(false);
        AtomicBoolean result2Closed = new AtomicBoolean(false);

        FetchResult result1 = new FetchResult(null, () -> result1Closed.set(true));
        FetchResult result2 = new FetchResult(null, () -> result2Closed.set(true));

        FetchContext context = new FetchContext();
        context.add(result1);
        context.add(result2);
        context.close();

        assertTrue("FetchContext must close the first FetchResult", result1Closed.get());
        assertTrue("FetchContext must close the second FetchResult", result2Closed.get());
    }

    @Test
    public void testAddNullResultIsIgnored() {
        try (FetchContext context = new FetchContext()) {
            context.add(null);
            assertTrue(
                    "Sources should remain empty when a null result is added",
                    context.getSources().isEmpty());
        }
    }

    @Test
    public void testCloseHandlesIOExceptionAndContinues() {
        AtomicBoolean secondResultClosed = new AtomicBoolean(false);

        FetchResult failingResult = new FetchResult(null, () -> {
            throw new java.io.IOException("Simulated cleanup failure");
        });

        FetchResult succeedingResult = new FetchResult(null, () -> secondResultClosed.set(true));

        FetchContext context = new FetchContext();
        context.add(failingResult);
        context.add(succeedingResult);
        context.close();

        assertTrue("FetchContext must continue closing remaining results after an exception", secondResultClosed.get());
    }
}
