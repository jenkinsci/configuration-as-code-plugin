package io.jenkins.plugins.casc.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import io.jenkins.plugins.casc.CasCReloadListener;
import io.jenkins.plugins.casc.history.CasCHistoryAction.HistoryEntry;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

public class LocalFileHistoryBackendTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testSaveCreatesFilesAndMetadataCorrectly() throws Exception {
        Thread.sleep(2000);
        File baseHistoryDir = new File(j.jenkins.getRootDir(), "casc-history");
        hudson.Util.deleteRecursive(baseHistoryDir);

        LocalFileHistoryBackend backend = new LocalFileHistoryBackend();
        String yamlContent = "jenkins:\n  systemMessage: 'Hello World'";
        String user = "admin_user";

        backend.save(yamlContent, user);

        assertTrue("Base history directory should be created", baseHistoryDir.exists());

        File[] subDirs = baseHistoryDir.listFiles(File::isDirectory);
        assertEquals("Should have exactly one history entry folder", 1, Objects.requireNonNull(subDirs).length);

        File specificHistoryDir = subDirs[0];
        File yamlFile = new File(specificHistoryDir, "jenkins.yaml");
        File xmlFile = new File(specificHistoryDir, "history.xml");

        assertTrue("jenkins.yaml should exist", yamlFile.exists());
        assertTrue("history.xml should exist", xmlFile.exists());

        String savedYaml = Files.readString(yamlFile.toPath());
        assertEquals("YAML content should match exactly", yamlContent, savedYaml);

        String savedXml = Files.readString(xmlFile.toPath());
        assertTrue("XML should contain the user", savedXml.contains("<user>admin_user</user>"));
    }

    @Test
    public void testHistoryRetentionPolicyDeletesOldestEntries() throws Exception {
        Thread.sleep(2000);
        File baseHistoryDir = new File(j.jenkins.getRootDir(), "casc-history");
        hudson.Util.deleteRecursive(baseHistoryDir);
        assertTrue("Failed to create base history directory", baseHistoryDir.mkdirs() || baseHistoryDir.exists());

        LocalFileHistoryBackend backend = new LocalFileHistoryBackend();

        for (int i = 10; i <= 64; i++) {
            File dummyDir = new File(baseHistoryDir, "2026-01-01_12-00-" + i);
            assertTrue("Failed to create dummy directory " + dummyDir.getName(), dummyDir.mkdirs());
            Files.writeString(new File(dummyDir, "dummy.txt").toPath(), "test data");
        }

        assertEquals(55, Objects.requireNonNull(baseHistoryDir.listFiles(File::isDirectory)).length);

        backend.save("dummy yaml", "admin");

        File[] remainingDirs = baseHistoryDir.listFiles(File::isDirectory);
        assertEquals(
                "Should retain exactly 50 directories after cleanup", 50, Objects.requireNonNull(remainingDirs).length);

        for (int i = 10; i <= 15; i++) {
            File deletedDir = new File(baseHistoryDir, "2026-01-01_12-00-" + i);
            assertFalse("Oldest directory " + deletedDir.getName() + " should have been deleted", deletedDir.exists());
        }

        File survivedDir = new File(baseHistoryDir, "2026-01-01_12-00-16");
        assertTrue("Directory 16 should have survived the cleanup", survivedDir.exists());

        File newestDummyDir = new File(baseHistoryDir, "2026-01-01_12-00-64");
        assertTrue("Directory 64 should have survived the cleanup", newestDummyDir.exists());
    }

    @Test
    public void testXmlEscapingWithSpecialCharacters() throws Exception {
        File baseDir = new File(j.jenkins.getRootDir(), "casc-history");
        hudson.Util.deleteRecursive(baseDir);

        LocalFileHistoryBackend backend = new LocalFileHistoryBackend();
        String problematicUser = "admin <danger> & 'quotes'";

        backend.save("dummy-yaml-content", problematicUser);

        File[] dirs = baseDir.listFiles(File::isDirectory);

        assertTrue("History directory should exist", dirs != null && dirs.length > 0);
        File latestDir = dirs[0];

        String xml = Files.readString(new File(latestDir, "history.xml").toPath());

        assertTrue("Less-than should be escaped", xml.contains("&lt;danger&gt;"));
        assertTrue("Ampersand should be escaped", xml.contains("&amp;"));
        assertTrue("Single quote should be escaped", xml.contains("&apos;quotes&apos;"));
    }

    @Test
    public void testSaveHandlesTimestampCollisionsCorrectly() throws Exception {
        Thread.sleep(2000);
        File baseHistoryDir = new File(j.jenkins.getRootDir(), "casc-history");
        hudson.Util.deleteRecursive(baseHistoryDir);
        assertTrue(baseHistoryDir.mkdirs() || baseHistoryDir.exists());

        final String FROZEN_TIME = "2026-01-01_12-00-00";
        LocalFileHistoryBackend backend = new LocalFileHistoryBackend() {
            @Override
            String getCurrentTimestamp() {
                return FROZEN_TIME;
            }
        };

        File collisionDir = new File(baseHistoryDir, FROZEN_TIME);
        assertTrue("Failed to create manual collision directory", collisionDir.mkdirs());

        backend.save("colliding-content", "user1");

        File[] dirs = baseHistoryDir.listFiles(File::isDirectory);
        assertNotNull(dirs);
        assertEquals("Should have 2 directories (the manual one and the auto-incremented one)", 2, dirs.length);

        File incrementedDir = new File(baseHistoryDir, FROZEN_TIME + "_1");
        assertTrue("The directory with _1 suffix should have been created", incrementedDir.exists());

        String savedYaml = Files.readString(new File(incrementedDir, "jenkins.yaml").toPath());
        assertEquals("The incremented directory should contain the correct YAML", "colliding-content", savedYaml);
    }

    @Test
    public void testGetHistoryEntriesReturnsEmptyListWhenNoHistoryExists() throws IOException {
        File baseHistoryDir = new File(j.jenkins.getRootDir(), "casc-history");
        hudson.Util.deleteRecursive(baseHistoryDir);

        CasCHistoryAction action = new CasCHistoryAction();

        List<HistoryEntry> entries = action.getHistoryEntries();

        assertNotNull("Entries list should not be null", entries);
        Assert.assertTrue("Entries list should be empty", entries.isEmpty());
    }

    @Test
    public void testGetHistoryEntriesReturnsNewestFirst() throws Exception {
        File baseHistoryDir = new File(j.jenkins.getRootDir(), "casc-history");
        hudson.Util.deleteRecursive(baseHistoryDir);
        assertTrue(baseHistoryDir.mkdirs() || baseHistoryDir.exists());

        String oldest = "2026-01-01_10-00-00";
        String middle = "2026-01-01_11-00-00";
        String newest = "2026-01-01_12-00-00";

        assertTrue(new File(baseHistoryDir, middle).mkdirs());
        assertTrue(new File(baseHistoryDir, oldest).mkdirs());
        assertTrue(new File(baseHistoryDir, newest).mkdirs());

        Files.writeString(
                new File(new File(baseHistoryDir, oldest), "history.xml").toPath(),
                "<history><user>Alice</user></history>");
        Files.writeString(
                new File(new File(baseHistoryDir, middle), "history.xml").toPath(),
                "<history><user>Bob</user></history>");
        Files.writeString(
                new File(new File(baseHistoryDir, newest), "history.xml").toPath(),
                "<history><user>Charlie</user></history>");

        CasCHistoryAction action = new CasCHistoryAction();

        List<HistoryEntry> entries = action.getHistoryEntries();

        Assert.assertEquals("Should have exactly 3 entries", 3, entries.size());

        Assert.assertEquals(
                "Newest entry should be first", newest, entries.get(0).timestamp());
        Assert.assertEquals("Charlie", entries.get(0).user());

        Assert.assertEquals(
                "Middle entry should be second", middle, entries.get(1).timestamp());
        Assert.assertEquals("Bob", entries.get(1).user());

        Assert.assertEquals(
                "Oldest entry should be last", oldest, entries.get(2).timestamp());
        Assert.assertEquals("Alice", entries.get(2).user());
    }

    @Test
    public void testConcurrentSavesDoNotCorruptDataOrThrowExceptions() throws Exception {
        Thread.sleep(2000);
        File baseDir = new File(j.jenkins.getRootDir(), "casc-history");
        hudson.Util.deleteRecursive(baseDir);

        LocalFileHistoryBackend backend = new LocalFileHistoryBackend();
        int threadCount = 20;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startingGun = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final String user = "user-" + i;
            final String yaml = "content-" + i;

            futures.add(executor.submit(() -> {
                startingGun.await();
                backend.save(yaml, user);
                return null;
            }));
        }

        startingGun.countDown();

        for (Future<Void> future : futures) {
            future.get();
        }
        executor.shutdown();

        File[] dirs = baseDir.listFiles(File::isDirectory);

        assertNotNull(dirs);
        assertEquals(
                "Should have successfully created all 20 history entries without overwriting each other",
                threadCount,
                dirs.length);
    }

    @Test
    public void testReloadListenerExceptionDoesNotBreakOthers() {
        GoodListener.called = false;
        BadListener.shouldThrow = true;

        try {
            CasCReloadListener.fire();

            assertTrue("Other listeners should still execute even if one throws an exception", GoodListener.called);
        } finally {
            BadListener.shouldThrow = false;
        }
    }

    @TestExtension
    public static class BadListener implements CasCReloadListener {
        static boolean shouldThrow = false;

        @Override
        public void onConfigurationReloaded() {
            if (shouldThrow) {
                throw new RuntimeException("Boom!");
            }
        }
    }

    @TestExtension
    public static class GoodListener implements CasCReloadListener {
        static boolean called = false;

        @Override
        public void onConfigurationReloaded() {
            called = true;
        }
    }

    @Test
    public void testSaveThrowsExceptionWhenDirectoryCreationFails() throws Exception {
        File jenkinsRoot = j.jenkins.getRootDir();
        File baseHistoryDir = new File(jenkinsRoot, "casc-history");

        hudson.Util.deleteRecursive(baseHistoryDir);

        boolean locked = jenkinsRoot.setWritable(false, false);

        assumeTrue("Test requires the OS to support setting directories to read-only", locked);

        try {
            LocalFileHistoryBackend backend = new LocalFileHistoryBackend();

            backend.save("dummy yaml", "admin");

            Assert.fail("Should have thrown an IOException because the directory couldn't be created!");
        } catch (IOException e) {
            assertTrue(
                    "Message should match the exception in the code",
                    e.getMessage().contains("Failed to create base history directory"));
        } finally {
            if (!jenkinsRoot.setWritable(true, false)) {
                System.err.println("WARNING: Failed to restore writable state to Jenkins root directory!");
            }
        }
    }

    @Test
    public void testSaveThrowsExceptionWhenSpecificDirectoryCreationFails() throws Exception {
        File baseHistoryDir = new File(j.jenkins.getRootDir(), "casc-history");

        hudson.Util.deleteRecursive(baseHistoryDir);
        assertTrue("Setup failed: could not create base directory", baseHistoryDir.mkdirs());

        boolean locked = baseHistoryDir.setWritable(false, false);
        assumeTrue("Test requires the OS to support setting directories to read-only", locked);

        try {
            LocalFileHistoryBackend backend = new LocalFileHistoryBackend();

            backend.save("dummy yaml", "admin");

            Assert.fail("Should have thrown an IOException because the specific directory couldn't be created!");
        } catch (IOException e) {
            assertTrue(
                    "Message should match the specific directory exception in the code. Actual message: "
                            + e.getMessage(),
                    e.getMessage().contains("Failed to create specific history directory"));
        } finally {
            if (!baseHistoryDir.setWritable(true, false)) {
                System.err.println("WARNING: Failed to restore writable state to base history directory!");
            }
        }
    }

    @Test
    public void testExceptionLoggedWhenOldDirectoryCannotBeDeleted() throws Exception {
        Thread.sleep(2000);

        File baseHistoryDir = new File(j.jenkins.getRootDir(), "casc-history");
        hudson.Util.deleteRecursive(baseHistoryDir);
        assertTrue(baseHistoryDir.mkdirs() || baseHistoryDir.exists());

        LocalFileHistoryBackend backend = new LocalFileHistoryBackend();

        for (int i = 10; i <= 64; i++) {
            File dummyDir = new File(baseHistoryDir, "2026-01-01_12-00-" + i);
            assertTrue(dummyDir.mkdirs());
            Files.writeString(new File(dummyDir, "dummy.txt").toPath(), "test data");
        }

        File probeDir = new File(baseHistoryDir, "probe-dir");
        assertTrue(probeDir.mkdirs());
        Files.writeString(new File(probeDir, "probe.txt").toPath(), "probe data");
        assumeTrue("OS must support locking directories", probeDir.setWritable(false, false));

        boolean osRespectsLocks;
        try {
            hudson.Util.deleteRecursive(probeDir);
            osRespectsLocks = probeDir.exists();
        } catch (IOException e) {
            osRespectsLocks = true;
        }

        assumeTrue("OS ignores directory locks, gracefully skipping test", osRespectsLocks);

        File oldestDir = new File(baseHistoryDir, "2026-01-01_12-00-10");

        boolean locked = oldestDir.setWritable(false, false);
        assumeTrue("Test requires the OS to support setting directories to read-only", locked);

        try {
            backend.save("dummy yaml", "admin");
            assertTrue("Oldest directory should still exist because deletion was blocked", oldestDir.exists());
        } finally {
            if (!oldestDir.setWritable(true, false)) {
                System.err.println("WARNING: Failed to restore writable state to the oldest directory!");
            }
        }
    }

    @Test
    public void testGetHistoryEntriesHandlesCorruptXmlGracefully() throws Exception {
        File baseHistoryDir = new File(j.jenkins.getRootDir(), "casc-history");
        hudson.Util.deleteRecursive(baseHistoryDir);
        assertTrue(baseHistoryDir.mkdirs() || baseHistoryDir.exists());

        String timestamp = "2026-01-01_12-00-00";
        File specificHistoryDir = new File(baseHistoryDir, timestamp);
        assertTrue(specificHistoryDir.mkdirs());

        File xmlFile = new File(specificHistoryDir, "history.xml");
        Files.writeString(xmlFile.toPath(), "<history><user>admin</use");

        File yamlFile = new File(specificHistoryDir, "jenkins.yaml");
        Files.writeString(yamlFile.toPath(), "jenkins:\n  systemMessage: 'test'");

        CasCHistoryAction action = new CasCHistoryAction();
        List<HistoryEntry> entries = action.getHistoryEntries();

        assertEquals("Should have exactly 1 entry", 1, entries.size());
        assertEquals("Timestamp should match", timestamp, entries.get(0).timestamp());
        assertEquals(
                "User should fallback to Unknown User when XML is unreadable",
                "Unknown User",
                entries.get(0).user());
    }

    @Test
    public void testHistoryEntryFormattedTimestamp() {
        HistoryEntry entry = new HistoryEntry("2026-04-09_14-30-05", "admin");

        LocalDateTime dt =
                LocalDateTime.parse("2026-04-09_14-30-05", DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String expected = dt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss"));

        assertEquals("Timestamp should be formatted correctly", expected, entry.getFormattedTimestamp());
    }

    @Test
    public void testDoViewReturns400WhenTimestampIsMissing() throws Exception {
        CasCHistoryAction action = new CasCHistoryAction();

        StaplerRequest2 req = (StaplerRequest2) java.lang.reflect.Proxy.newProxyInstance(
                StaplerRequest2.class.getClassLoader(), new Class[] {StaplerRequest2.class}, (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        switch (method.getName()) {
                            case "equals" -> {
                                return proxy == args[0];
                            }
                            case "hashCode" -> {
                                return System.identityHashCode(proxy);
                            }
                            case "toString" -> {
                                return "MockRequest";
                            }
                        }
                    }
                    return null;
                });

        int[] capturedErrorCode = new int[1];

        StaplerResponse2 res = (StaplerResponse2) Proxy.newProxyInstance(
                StaplerResponse2.class.getClassLoader(),
                new Class[] {StaplerResponse2.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        switch (method.getName()) {
                            case "equals" -> {
                                return proxy == args[0];
                            }
                            case "hashCode" -> {
                                return System.identityHashCode(proxy);
                            }
                            case "toString" -> {
                                return "MockResponse";
                            }
                        }
                    }
                    if ("sendError".equals(method.getName())) {
                        capturedErrorCode[0] = (int) args[0];
                    }
                    return null;
                });

        action.doView(req, res);
        assertEquals("Should return 400 Bad Request", 400, capturedErrorCode[0]);
    }

    @Test
    public void testDoViewReturns400WhenTimestampIsInvalid() throws Exception {
        CasCHistoryAction action = new CasCHistoryAction();

        StaplerRequest2 req = (StaplerRequest2) Proxy.newProxyInstance(
                StaplerRequest2.class.getClassLoader(), new Class[] {StaplerRequest2.class}, (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        switch (method.getName()) {
                            case "equals" -> {
                                return proxy == args[0];
                            }
                            case "hashCode" -> {
                                return System.identityHashCode(proxy);
                            }
                            case "toString" -> {
                                return "MockRequest";
                            }
                        }
                    }
                    if ("getParameter".equals(method.getName())) {
                        return "../../etc/passwd";
                    }
                    return null;
                });

        int[] capturedErrorCode = new int[1];
        StaplerResponse2 res = (StaplerResponse2) Proxy.newProxyInstance(
                StaplerResponse2.class.getClassLoader(),
                new Class[] {StaplerResponse2.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        switch (method.getName()) {
                            case "equals" -> {
                                return proxy == args[0];
                            }
                            case "hashCode" -> {
                                return System.identityHashCode(proxy);
                            }
                            case "toString" -> {
                                return "MockResponse";
                            }
                        }
                    }
                    if ("sendError".equals(method.getName())) {
                        capturedErrorCode[0] = (int) args[0];
                    }
                    return null;
                });

        action.doView(req, res);
        assertEquals("Should return 400 Bad Request", 400, capturedErrorCode[0]);
    }

    @Test
    public void testDoViewReturns404WhenRecordDoesNotExist() throws Exception {
        CasCHistoryAction action = new CasCHistoryAction();

        StaplerRequest2 req = (StaplerRequest2) Proxy.newProxyInstance(
                StaplerRequest2.class.getClassLoader(),
                new Class[] {org.kohsuke.stapler.StaplerRequest2.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        switch (method.getName()) {
                            case "equals" -> {
                                return proxy == args[0];
                            }
                            case "hashCode" -> {
                                return System.identityHashCode(proxy);
                            }
                            case "toString" -> {
                                return "MockRequest";
                            }
                        }
                    }
                    if ("getParameter".equals(method.getName())) {
                        return "2026-01-01_12-00-00";
                    }
                    return null;
                });

        int[] capturedErrorCode = new int[1];
        StaplerResponse2 res = (StaplerResponse2) Proxy.newProxyInstance(
                StaplerResponse2.class.getClassLoader(),
                new Class[] {StaplerResponse2.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        switch (method.getName()) {
                            case "equals" -> {
                                return proxy == args[0];
                            }
                            case "hashCode" -> {
                                return System.identityHashCode(proxy);
                            }
                            case "toString" -> {
                                return "MockResponse";
                            }
                        }
                    }
                    if ("sendError".equals(method.getName())) {
                        capturedErrorCode[0] = (int) args[0];
                    }
                    return null;
                });

        action.doView(req, res);
        assertEquals("Should return 404 Not Found", 404, capturedErrorCode[0]);
    }

    @Test
    public void testDoViewSuccessfullyReturnsYamlContent() throws Exception {
        File baseHistoryDir = new File(j.jenkins.getRootDir(), "casc-history");
        hudson.Util.deleteRecursive(baseHistoryDir);

        String timestamp = "2026-01-01_12-00-00";
        File specificDir = new File(baseHistoryDir, timestamp);
        assertTrue(specificDir.mkdirs());
        File yamlFile = new File(specificDir, "jenkins.yaml");
        Files.writeString(yamlFile.toPath(), "jenkins:\n  systemMessage: 'Testing'");

        CasCHistoryAction action = new CasCHistoryAction();

        StaplerRequest2 req = (StaplerRequest2) java.lang.reflect.Proxy.newProxyInstance(
                StaplerRequest2.class.getClassLoader(), new Class[] {StaplerRequest2.class}, (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        switch (method.getName()) {
                            case "equals" -> {
                                return proxy == args[0];
                            }
                            case "hashCode" -> {
                                return System.identityHashCode(proxy);
                            }
                            case "toString" -> {
                                return "MockRequest";
                            }
                        }
                    }
                    if ("getParameter".equals(method.getName())) {
                        return timestamp;
                    }
                    return null;
                });

        String[] capturedContentType = new String[1];

        try (ServletOutputStream dummyStream = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {}

            @Override
            public void write(int b) {}
        }) {
            StaplerResponse2 res = (StaplerResponse2) Proxy.newProxyInstance(
                    StaplerResponse2.class.getClassLoader(),
                    new Class[] {StaplerResponse2.class},
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            switch (method.getName()) {
                                case "equals" -> {
                                    return proxy == args[0];
                                }
                                case "hashCode" -> {
                                    return System.identityHashCode(proxy);
                                }
                                case "toString" -> {
                                    return "MockResponse";
                                }
                            }
                        }
                        if ("setContentType".equals(method.getName())) {
                            capturedContentType[0] = (String) args[0];
                        }
                        if ("getOutputStream".equals(method.getName())) {
                            return dummyStream;
                        }
                        return null;
                    });

            action.doView(req, res);
        }

        assertEquals("Content type should match", "text/plain;charset=UTF-8", capturedContentType[0]);
    }
}
