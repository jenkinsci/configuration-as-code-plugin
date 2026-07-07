package io.jenkins.plugins.casc.fetcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LocalFileSystemFetcherTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private final LocalFileSystemFetcher fetcher = new LocalFileSystemFetcher();

    @Test
    public void testSupportsLogic() throws IOException {
        File tempFile = tempFolder.newFile("test.yaml");

        assertTrue("Should support valid file:// URI", fetcher.supports("file:///some/path"));
        assertTrue("Should support valid local paths", fetcher.supports(tempFile.getAbsolutePath()));
        assertFalse("Should reject null locations", fetcher.supports(null));
        assertFalse("Should reject non-existent paths", fetcher.supports("/does/not/exist/ever.yaml"));
    }

    @Test
    public void testFetchSingleFile() throws Exception {
        File file = tempFolder.newFile("single.yaml");
        Files.write(file.toPath(), "jenkins:".getBytes());

        FetchResult result = fetcher.fetch(file.getAbsolutePath(), null);
        List<ResolvedYaml> items = result.items();

        assertEquals(1, items.size());
        assertEquals("single.yaml", items.get(0).relativePath());
    }

    @Test
    public void testFetchDirectoryIgnoresHiddenAndNonYaml() throws Exception {

        File dir = tempFolder.newFolder("casc-configs");
        File valid1 = new File(dir, "a.yaml");
        File valid2 = new File(dir, "b.yml");
        File hidden = new File(dir, ".secrets.yaml");
        File txtFile = new File(dir, "readme.txt");

        Files.write(valid1.toPath(), "jenkins:".getBytes());
        Files.write(valid2.toPath(), "unclassified:".getBytes());
        Files.write(hidden.toPath(), "secret: 123".getBytes());
        Files.write(txtFile.toPath(), "hello".getBytes());

        FetchResult result = fetcher.fetch(dir.getAbsolutePath(), null);
        List<ResolvedYaml> items = result.items();

        assertEquals("Should only find a.yaml and b.yml", 2, items.size());

        boolean hasA = items.stream().anyMatch(i -> i.relativePath().equals("a.yaml"));
        boolean hasB = items.stream().anyMatch(i -> i.relativePath().equals("b.yml"));

        assertTrue(hasA);
        assertTrue(hasB);
    }

    @Test
    public void testNestedDirectoryTraversal() throws Exception {
        File rootDir = tempFolder.newFolder("configs");
        File prodDir = new File(rootDir, "prod");
        assertTrue("Failed to create nested directory", prodDir.mkdir());

        File jenkinsYaml = new File(prodDir, "jenkins.yaml");
        Files.write(jenkinsYaml.toPath(), "jenkins:\n  mode: EXCLUSIVE".getBytes());

        FetchResult result = fetcher.fetch(rootDir.getAbsolutePath(), null);
        List<ResolvedYaml> items = result.items();

        assertEquals(1, items.size());
        assertEquals("prod/jenkins.yaml", items.get(0).relativePath());
    }

    @Test
    public void testResolvedYamlToString() {
        ResolvedYaml yaml = new ResolvedYaml("my/test/config.yaml", () -> null);

        assertEquals("ResolvedYaml{relativePath='my/test/config.yaml'}", yaml.toString());
    }

    @Test
    public void testSupportsThrowsExceptionOnInvalidCharacters() {
        assertFalse(
                "Should safely catch exception and return false for strictly invalid path strings",
                fetcher.supports("invalid\u0000path"));
    }

    @Test(expected = IOException.class)
    public void testFetchThrowsOnInvalidFileUri() throws Exception {
        fetcher.fetch("file:///invalid path with spaces.yaml", null);
    }

    @Test(expected = IOException.class)
    public void testFetchThrowsOnNonExistentPath() throws Exception {
        fetcher.fetch("/this/path/absolutely/does/not/exist/casc.yaml", null);
    }
}
