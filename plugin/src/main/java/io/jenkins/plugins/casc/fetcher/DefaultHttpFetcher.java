package io.jenkins.plugins.casc.fetcher;

import static java.lang.Thread.currentThread;

import hudson.Extension;
import hudson.ProxyConfiguration;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;

@Extension(ordinal = -100)
public class DefaultHttpFetcher implements CasCConfigFetcher {

    @FunctionalInterface
    public interface HttpSettingsProvider {
        CascHttpSettings.RemoteConfig getConfigForUrl(String url);
    }

    private final HttpSettingsProvider settingsProvider;

    public DefaultHttpFetcher() {
        this(CascHttpSettings::getConfigForUrl);
    }

    DefaultHttpFetcher(HttpSettingsProvider settingsProvider) {
        this.settingsProvider = settingsProvider;
    }

    @Override
    public boolean supports(String location) {
        return location != null && (location.startsWith("http://") || location.startsWith("https://"));
    }

    @Override
    public FetchResult fetch(String location, FetchCredentials credentials) throws IOException {
        URI requestUri;
        try {
            requestUri = new URI(location);
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URL: " + location, e);
        }

        String path = requestUri.getPath();
        String fileName =
                (path != null && path.contains("/")) ? path.substring(path.lastIndexOf('/') + 1) : "casc.yaml";

        if (fileName.isEmpty()) {
            fileName = "casc.yaml";
        }

        HttpClient client = ProxyConfiguration.newHttpClient();

        HttpRequest.Builder requestBuilder =
                ProxyConfiguration.newHttpRequestBuilder(requestUri).GET().timeout(Duration.ofSeconds(30));

        CascHttpSettings.RemoteConfig remoteConfig = settingsProvider.getConfigForUrl(location);

        if (remoteConfig != null && remoteConfig.getAuthMethod() != CascHttpSettings.AuthMethod.NONE) {
            if (credentials == null) {
                throw new IOException(
                        "Credentials required for " + location + " but no credential resolver was provided.");
            }
            applyAuthentication(requestBuilder, remoteConfig, credentials);
        }

        HttpRequest request = requestBuilder.build();
        byte[] yamlBytes;

        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Failed to fetch configuration from " + requestUri + ". HTTP status code: "
                        + response.statusCode());
            }

            yamlBytes = response.body();
        } catch (InterruptedException e) {
            currentThread().interrupt();
            throw new IOException("Interrupted while fetching configuration from: " + requestUri, e);
        }

        ResolvedYaml resolved = new ResolvedYaml(fileName, () -> new ByteArrayInputStream(yamlBytes));

        return new FetchResult(Collections.singletonList(resolved), (AutoCloseable) null);
    }

    private void applyAuthentication(
            HttpRequest.Builder requestBuilder, CascHttpSettings.RemoteConfig config, FetchCredentials credentials)
            throws IOException {
        String credentialId = config.getCredentialId();

        switch (config.getAuthMethod()) {
            case NONE:
                break;

            case BASIC:
                FetchAuthData.UsernamePassword userPass =
                        credentials.get(credentialId, FetchAuthData.UsernamePassword.class);
                if (userPass == null) {
                    throw new IOException(
                        "Unable to resolve Username/Password for ID: " + credentialId);
                }
                String authString = userPass.getUsername() + ":" + userPass.getPassword();
                String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));
                requestBuilder.header("Authorization", "Basic " + encodedAuth);
                break;

            case BEARER:
                FetchAuthData.Token token = credentials.get(credentialId, FetchAuthData.Token.class);
                if (token == null) {
                    throw new IOException("Unable to resolve Token for ID: " + credentialId);
                }
                requestBuilder.header("Authorization", "Bearer " + token.getToken());
                break;

            case API_KEY:
                FetchAuthData.Token apiKey = credentials.get(credentialId, FetchAuthData.Token.class);
                if (apiKey == null) {
                    throw new IOException("Unable to resolve API Key for ID: " + credentialId);
                }

                String headerName = config.getHeaderName();
                if (headerName == null || headerName.trim().isEmpty()) {
                    headerName = "x-api-key";
                }

                requestBuilder.header(headerName, apiKey.getToken());
                break;

            default:
                throw new IOException("Unsupported authentication method: " + config.getAuthMethod());
        }
    }
}
