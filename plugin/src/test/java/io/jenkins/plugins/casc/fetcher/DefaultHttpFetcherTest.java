package io.jenkins.plugins.casc.fetcher;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
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

    @Test
    public void testFetchWithTokenCredential() throws Exception {
        stubFor(get(urlEqualTo("/private.yaml"))
                .withHeader("Authorization", equalTo("Bearer secret-token"))
                .willReturn(aResponse().withStatus(200).withBody("jenkins:\n  systemMessage: 'Private'")));

        FetchCredentials credentials = new FetchCredentials() {
            @Override
            public <T extends FetchAuthData> T get(String credentialId, Class<T> type) {
                if ("MY_TOKEN".equals(credentialId) && type == FetchAuthData.Token.class) {
                    return type.cast((FetchAuthData.Token) () -> "secret-token");
                }
                return null;
            }
        };

        String targetUrl = wireMockRule.baseUrl() + "/private.yaml?cascCredentialId=MY_TOKEN";

        FetchResult result = fetcher.fetch(targetUrl, credentials);

        assertEquals(1, result.items().size());

        verify(getRequestedFor(urlEqualTo("/private.yaml"))
                .withHeader("Authorization", equalTo("Bearer secret-token")));
    }

    @Test
    public void testFetchWithUsernamePasswordCredential() throws Exception {
        stubFor(get(urlEqualTo("/private.yaml"))
                .withHeader(
                        "Authorization",
                        equalTo("Basic "
                                + java.util.Base64.getEncoder().encodeToString("user:password".getBytes(UTF_8))))
                .willReturn(aResponse().withStatus(200).withBody("jenkins:\n  systemMessage: 'Private'")));

        FetchCredentials credentials = new FetchCredentials() {
            @Override
            public <T extends FetchAuthData> T get(String credentialId, Class<T> type) {
                if ("MY_CREDENTIALS".equals(credentialId) && type == FetchAuthData.UsernamePassword.class) {
                    return type.cast(new FetchAuthData.UsernamePassword() {
                        @Override
                        public String getUsername() {
                            return "user";
                        }

                        @Override
                        public String getPassword() {
                            return "password";
                        }
                    });
                }
                return null;
            }
        };

        String targetUrl = wireMockRule.baseUrl() + "/private.yaml?cascCredentialId=MY_CREDENTIALS";

        FetchResult result = fetcher.fetch(targetUrl, credentials);

        assertEquals(1, result.items().size());

        verify(getRequestedFor(urlEqualTo("/private.yaml"))
                .withHeader(
                        "Authorization",
                        equalTo("Basic "
                                + java.util.Base64.getEncoder().encodeToString("user:password".getBytes(UTF_8)))));
    }

    @Test
    public void testCredentialIdIsRemovedFromRequestUrl() throws Exception {
        stubFor(get(urlEqualTo("/config.yaml?foo=bar"))
                .withHeader("Authorization", equalTo("Bearer secret-token"))
                .willReturn(aResponse().withStatus(200).withBody("jenkins: {}")));

        FetchCredentials credentials = new FetchCredentials() {
            @Override
            public <T extends FetchAuthData> T get(String credentialId, Class<T> type) {
                if ("MY_TOKEN".equals(credentialId) && type == FetchAuthData.Token.class) {
                    return type.cast((FetchAuthData.Token) () -> "secret-token");
                }
                return null;
            }
        };

        String targetUrl = wireMockRule.baseUrl() + "/config.yaml?foo=bar&cascCredentialId=MY_TOKEN";

        fetcher.fetch(targetUrl, credentials);

        // FIXED: Using getRequestedFor
        verify(getRequestedFor(urlEqualTo("/config.yaml?foo=bar"))
                .withHeader("Authorization", equalTo("Bearer secret-token")));
    }

    @Test
    public void testFetchThrowsWhenCredentialCannotBeResolved() {
        FetchCredentials credentials = new FetchCredentials() {
            @Override
            public <T extends FetchAuthData> T get(String credentialId, Class<T> type) {
                return null;
            }
        };

        String targetUrl = wireMockRule.baseUrl() + "/private.yaml?cascCredentialId=UNKNOWN";

        IOException e = assertThrows(IOException.class, () -> fetcher.fetch(targetUrl, credentials));

        assertTrue(e.getMessage().contains("Unable to resolve credentials with ID 'UNKNOWN'"));
    }

    @Test
    public void testFetchThrowsWhenCredentialResolverIsMissing() {
        String targetUrl = wireMockRule.baseUrl() + "/private.yaml?cascCredentialId=MY_TOKEN";

        IOException e = assertThrows(IOException.class, () -> fetcher.fetch(targetUrl, null));

        assertTrue(e.getMessage().contains("no credential resolver was provided"));
    }

    @Test
    public void testFetchWithCredentialIdParameter() throws Exception {
        stubFor(get(urlEqualTo("/private.yaml"))
                .withHeader("Authorization", equalTo("Bearer secret-token"))
                .willReturn(aResponse().withStatus(200).withBody("jenkins: {}")));

        FetchCredentials credentials = new FetchCredentials() {
            @Override
            public <T extends FetchAuthData> T get(String credentialId, Class<T> type) {
                if ("MY_TOKEN".equals(credentialId) && type == FetchAuthData.Token.class) {
                    return type.cast((FetchAuthData.Token) () -> "secret-token");
                }
                return null;
            }
        };

        fetcher.fetch(wireMockRule.baseUrl() + "/private.yaml?credentialId=MY_TOKEN", credentials);

        verify(getRequestedFor(urlEqualTo("/private.yaml"))
                .withHeader("Authorization", equalTo("Bearer secret-token")));
    }
}
