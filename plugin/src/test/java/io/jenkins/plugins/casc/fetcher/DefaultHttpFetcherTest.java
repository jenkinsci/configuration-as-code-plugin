package io.jenkins.plugins.casc.fetcher;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.stream.Collectors;
import org.junit.Test;

public class DefaultHttpFetcherTest {

    private final DefaultHttpFetcher fetcher = new DefaultHttpFetcher();

    @Test
    public void testSupportsLogic() {
        assertTrue(fetcher.supports("http://example.com/casc.yaml"));
        assertTrue(fetcher.supports("https://github.com/org/repo/jenkins.yml"));
        assertFalse(fetcher.supports("file:///local/path"));
        assertFalse(fetcher.supports(null));
    }

    @Test(expected = IOException.class)
    public void testFetchThrowsOnInvalidUrl() throws Exception {
        fetcher.fetch("http://example.com/invalid path with spaces", null);
    }

    @Test
    public void testFilenameExtraction() {
        assertEquals("casc.yaml", extractFilename("http://example.com"));
        assertEquals("casc.yaml", extractFilename("http://example.com/"));
        assertEquals("jenkins.yaml", extractFilename("https://example.com/path/to/jenkins.yaml"));
        assertEquals("config.yml", extractFilename("https://example.com/config.yml?token=123"));
    }

    @Test
    public void testActualHttpFetchIntegration() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/casc.yaml", exchange -> {
            byte[] response = "jenkins:\n  systemMessage: 'Hello HTTP'".getBytes(UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            String targetUrl = "http://localhost:" + port + "/casc.yaml";

            FetchResult result = fetcher.fetch(targetUrl, null);

            assertEquals(1, result.items().size());
            ResolvedYaml yaml = result.items().get(0);

            assertEquals("casc.yaml", yaml.relativePath());

            String content = new BufferedReader(new InputStreamReader(yaml.open(), UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            assertEquals("jenkins:\n  systemMessage: 'Hello HTTP'", content);

        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testFetchWithEmptyFileNameUsesDefault() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            byte[] response = "jenkins:\n  systemMessage: 'Fallback'".getBytes(UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            String targetUrl = "http://localhost:" + port + "/";

            FetchResult result = fetcher.fetch(targetUrl, null);

            assertEquals(1, result.items().size());
            ResolvedYaml yaml = result.items().get(0);

            assertEquals("casc.yaml", yaml.relativePath());

        } finally {
            server.stop(0);
        }
    }

    private String extractFilename(String location) {
        try {
            URI uri = new URI(location);
            String path = uri.getPath();
            if (path != null && path.contains("/")) {
                String name = path.substring(path.lastIndexOf('/') + 1);
                return name.isEmpty() ? "casc.yaml" : name;
            }
            return "casc.yaml";
        } catch (Exception e) {
            return "casc.yaml";
        }
    }
}
