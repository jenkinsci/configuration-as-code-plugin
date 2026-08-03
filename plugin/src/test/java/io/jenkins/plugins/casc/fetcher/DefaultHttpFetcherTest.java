package io.jenkins.plugins.casc.fetcher;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.interrupted;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;

public class DefaultHttpFetcherTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

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
    public void testActualHttpFetchIntegration() throws Exception {
        stubFor(get(urlEqualTo("/casc.yaml"))
                .willReturn(aResponse().withStatus(200).withBody("jenkins:\n  systemMessage: 'Hello HTTP'")));

        String targetUrl = wireMockRule.baseUrl() + "/casc.yaml";
        FetchResult result = fetcher.fetch(targetUrl, null);

        assertEquals(1, result.items().size());
        ResolvedYaml yaml = result.items().get(0);

        assertEquals("casc.yaml", yaml.relativePath());

        String content = new BufferedReader(new InputStreamReader(yaml.open(), UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

        assertEquals("jenkins:\n  systemMessage: 'Hello HTTP'", content);
    }

    @Test
    public void testFetchWithEmptyFileNameUsesDefault() throws Exception {
        stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse().withStatus(200).withBody("jenkins:\n  systemMessage: 'Fallback'")));

        String targetUrl = wireMockRule.baseUrl() + "/";
        FetchResult result = fetcher.fetch(targetUrl, null);

        assertEquals(1, result.items().size());
        ResolvedYaml yaml = result.items().get(0);

        assertEquals("casc.yaml", yaml.relativePath());
    }

    @Test
    public void testFetchThrowsOnHttpError() {
        stubFor(get(urlEqualTo("/404")).willReturn(aResponse().withStatus(404)));

        String targetUrl = wireMockRule.baseUrl() + "/404";

        IOException e = assertThrows(
                "Expected fetcher to throw IOException due to HTTP 404 status",
                IOException.class,
                () -> fetcher.fetch(targetUrl, null));

        assertTrue("Message should contain the HTTP status code", e.getMessage().contains("HTTP status code: 404"));
    }

    @Test
    public void testFetchThrowsOnInterruption() {
        stubFor(get(urlEqualTo("/slow")).willReturn(aResponse().withStatus(200).withFixedDelay(1000)));

        String targetUrl = wireMockRule.baseUrl() + "/slow";

        currentThread().interrupt();

        try {
            IOException e = assertThrows(
                    "Expected fetcher to throw IOException due to thread interruption",
                    IOException.class,
                    () -> fetcher.fetch(targetUrl, null));

            assertTrue(
                    "Message should indicate the thread was interrupted",
                    e.getMessage().contains("Interrupted while fetching"));

            assertTrue(
                    "Thread interrupt flag should be restored", currentThread().isInterrupted());
        } finally {
            @SuppressWarnings("unused")
            boolean cleared = interrupted();
        }
    }
}
