package io.jenkins.plugins.casc.fetcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

public class FetchResultTest {

    @Test
    public void testAutoCloseableLifecycle() throws Exception {
        AtomicBoolean wasClosed = new AtomicBoolean(false);
        AutoCloseable mockResource = () -> wasClosed.set(true);

        FetchResult result = new FetchResult(Collections.emptyList(), mockResource);

        result.close();

        assertTrue("FetchResult must trigger close() on wrapped AutoCloseables", wasClosed.get());
    }

    @Test
    public void testTempDirectoryCleanup() throws Exception {
        Path tempDir = Files.createTempDirectory("casc-test-cleanup-");
        Path nestedFile = tempDir.resolve("jenkins.yaml");
        Files.write(nestedFile, "jenkins:".getBytes());

        assertTrue(Files.exists(nestedFile));

        FetchResult result = new FetchResult(Collections.emptyList(), tempDir);

        result.close();

        assertFalse("Nested files must be deleted", Files.exists(nestedFile));
        assertFalse("The temporary directory itself must be deleted", Files.exists(tempDir));
    }

    @Test(expected = IOException.class)
    public void testCloseWrapsAndPropagatesExceptions() throws Exception {
        AutoCloseable faultyResource = () -> {
            throw new Exception("Simulated cleanup failure");
        };

        FetchResult result = new FetchResult(Collections.emptyList(), faultyResource);

        result.close();
    }

    @Test
    public void testCloseIgnoresNullsInLists() throws Exception {
        FetchResult result = new FetchResult(Collections.emptyList(), (AutoCloseable) null);

        Field resourcesField = FetchResult.class.getDeclaredField("resourcesToClose");
        resourcesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<AutoCloseable> resources = (List<AutoCloseable>) resourcesField.get(result);
        resources.add(null);

        Field pathsField = FetchResult.class.getDeclaredField("pathsToDelete");
        pathsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Path> paths = (List<Path>) pathsField.get(result);
        paths.add(null);
        result.close();
    }

    @Test
    public void testCloseSkipsNonExistentDirectory() throws Exception {
        Path tempDir = Files.createTempDirectory("casc-missing-");
        FetchResult result = new FetchResult(Collections.emptyList(), tempDir);
        Files.delete(tempDir);
        result.close();
    }
}
