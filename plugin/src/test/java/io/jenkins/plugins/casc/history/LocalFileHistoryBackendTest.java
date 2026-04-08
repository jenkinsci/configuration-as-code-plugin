package io.jenkins.plugins.casc.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.jenkins.plugins.casc.history.CasCHistoryAction.HistoryEntry;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

public class LocalFileHistoryBackendTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testSaveCreatesFilesAndMetadataCorrectly() throws Exception {
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
}
