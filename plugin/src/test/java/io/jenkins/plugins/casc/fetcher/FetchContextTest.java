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
}
