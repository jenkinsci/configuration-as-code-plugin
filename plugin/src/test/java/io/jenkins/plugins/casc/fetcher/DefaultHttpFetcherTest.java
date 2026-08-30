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
import static java.util.Base64.getEncoder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class DefaultHttpFetcherTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    private CascHttpSettings.RemoteConfig mockConfig;
    private DefaultHttpFetcher fetcher;

    @Before
    public void setup() {
        mockConfig = null;
        fetcher = new DefaultHttpFetcher(url -> mockConfig);
    }

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
    public void testFetchWithBearerToken() throws Exception {
        stubFor(get(urlEqualTo("/private.yaml"))
                .withHeader("Authorization", equalTo("Bearer secret-token"))
                .willReturn(aResponse().withStatus(200).withBody("jenkins:\n  systemMessage: 'Private'")));

        mockConfig =
                new CascHttpSettings.RemoteConfig("http://localhost", "MY_TOKEN", CascHttpSettings.AuthMethod.BEARER);

        FetchCredentials credentials = new FetchCredentials() {
            @Override
            public <T extends FetchAuthData> T get(String credentialId, Class<T> type) {
                if ("MY_TOKEN".equals(credentialId) && type == FetchAuthData.Token.class) {
                    return type.cast((FetchAuthData.Token) () -> "secret-token");
                }
                return null;
            }
        };

        String targetUrl = wireMockRule.baseUrl() + "/private.yaml";
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
                        equalTo("Basic " + getEncoder().encodeToString("user:password".getBytes(UTF_8))))
                .willReturn(aResponse().withStatus(200).withBody("jenkins:\n  systemMessage: 'Private'")));

        mockConfig = new CascHttpSettings.RemoteConfig(
                "http://localhost", "MY_CREDENTIALS", CascHttpSettings.AuthMethod.BASIC);

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

        String targetUrl = wireMockRule.baseUrl() + "/private.yaml";
        FetchResult result = fetcher.fetch(targetUrl, credentials);

        assertEquals(1, result.items().size());

        verify(getRequestedFor(urlEqualTo("/private.yaml"))
                .withHeader(
                        "Authorization",
                        equalTo("Basic " + getEncoder().encodeToString("user:password".getBytes(UTF_8)))));
    }

    @Test
    public void testFetchWithApiKeyCustomHeader() throws Exception {
        stubFor(get(urlEqualTo("/private.yaml"))
                .withHeader("X-Custom-Auth", equalTo("secret-key"))
                .willReturn(aResponse().withStatus(200).withBody("jenkins: {}")));

        mockConfig = new CascHttpSettings.RemoteConfig(
                "http://localhost", "MY_API_KEY", CascHttpSettings.AuthMethod.API_KEY);
        mockConfig.setHeaderName("X-Custom-Auth");

        FetchCredentials credentials = new FetchCredentials() {
            @Override
            public <T extends FetchAuthData> T get(String credentialId, Class<T> type) {
                if ("MY_API_KEY".equals(credentialId) && type == FetchAuthData.Token.class) {
                    return type.cast((FetchAuthData.Token) () -> "secret-key");
                }
                return null;
            }
        };

        fetcher.fetch(wireMockRule.baseUrl() + "/private.yaml", credentials);

        verify(getRequestedFor(urlEqualTo("/private.yaml")).withHeader("X-Custom-Auth", equalTo("secret-key")));
    }

    @Test
    public void testFetchWithApiKeyDefaultHeader() throws Exception {
        stubFor(get(urlEqualTo("/private.yaml"))
                .withHeader("x-api-key", equalTo("secret-key"))
                .willReturn(aResponse().withStatus(200).withBody("jenkins: {}")));

        mockConfig = new CascHttpSettings.RemoteConfig(
                "http://localhost", "MY_API_KEY", CascHttpSettings.AuthMethod.API_KEY);
        mockConfig.setHeaderName("");

        FetchCredentials credentials = new FetchCredentials() {
            @Override
            public <T extends FetchAuthData> T get(String credentialId, Class<T> type) {
                if ("MY_API_KEY".equals(credentialId) && type == FetchAuthData.Token.class) {
                    return type.cast((FetchAuthData.Token) () -> "secret-key");
                }
                return null;
            }
        };

        fetcher.fetch(wireMockRule.baseUrl() + "/private.yaml", credentials);

        verify(getRequestedFor(urlEqualTo("/private.yaml")).withHeader("x-api-key", equalTo("secret-key")));
    }

    @Test
    public void testFetchThrowsWhenCredentialCannotBeResolved() {
        mockConfig =
                new CascHttpSettings.RemoteConfig("http://localhost", "UNKNOWN", CascHttpSettings.AuthMethod.BEARER);

        FetchCredentials credentials = new FetchCredentials() {
            @Override
            public <T extends FetchAuthData> T get(String credentialId, Class<T> type) {
                return null;
            }
        };

        String targetUrl = wireMockRule.baseUrl() + "/private.yaml";

        IOException e = assertThrows(IOException.class, () -> fetcher.fetch(targetUrl, credentials));
        assertTrue(e.getMessage().contains("Unable to resolve Token for ID: UNKNOWN"));
    }

    @Test
    public void testFetchThrowsWhenCredentialResolverIsMissing() {
        mockConfig =
                new CascHttpSettings.RemoteConfig("http://localhost", "MY_TOKEN", CascHttpSettings.AuthMethod.BEARER);
        String targetUrl = wireMockRule.baseUrl() + "/private.yaml";

        IOException e = assertThrows(IOException.class, () -> fetcher.fetch(targetUrl, null));

        assertTrue(e.getMessage().contains("no credential resolver was provided"));
    }

    @Test
    public void testFetchUrlWithFragment() throws Exception {
        stubFor(get(urlEqualTo("/casc.yaml"))
                .willReturn(aResponse().withStatus(200).withBody("jenkins:\n  systemMessage: 'Fragment Test'")));

        String targetUrl = wireMockRule.baseUrl() + "/casc.yaml#section1";
        FetchResult result = fetcher.fetch(targetUrl, null);

        assertEquals(1, result.items().size());
        verify(getRequestedFor(urlEqualTo("/casc.yaml")));
    }

    @Test
    public void testFetchWithNoAuthentication() throws Exception {
        stubFor(get(urlEqualTo("/public.yaml"))
                .willReturn(aResponse().withStatus(200).withBody("jenkins: {}")));

        mockConfig = new CascHttpSettings.RemoteConfig("http://localhost", null, CascHttpSettings.AuthMethod.NONE);

        fetcher.fetch(wireMockRule.baseUrl() + "/public.yaml", null);

        verify(getRequestedFor(urlEqualTo("/public.yaml")).withoutHeader("Authorization"));
    }

    @Test
    public void testFetchWithoutMatchingConfiguration() throws Exception {
        stubFor(get(urlEqualTo("/public.yaml"))
                .willReturn(aResponse().withStatus(200).withBody("jenkins: {}")));

        fetcher.fetch(wireMockRule.baseUrl() + "/public.yaml", null);

        verify(getRequestedFor(urlEqualTo("/public.yaml")).withoutHeader("Authorization"));
    }
}
