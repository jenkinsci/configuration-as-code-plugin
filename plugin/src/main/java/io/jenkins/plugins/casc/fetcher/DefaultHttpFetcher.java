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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Extension(ordinal = -100)
public class DefaultHttpFetcher implements CasCConfigFetcher {

    private static final String CREDENTIAL_ID_PARAM = "cascCredentialId";
    private static final String FALLBACK_CREDENTIAL_ID_PARAM = "credentialId";

    @Override
    public boolean supports(String location) {
        return location != null && (location.startsWith("http://") || location.startsWith("https://"));
    }

    @Override
    public FetchResult fetch(String location, FetchCredentials credentials) throws IOException {
        URI originalUri;
        try {
            originalUri = new URI(location);
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URL: " + location, e);
        }

        String rawQuery = originalUri.getQuery();
        String credentialId = null;
        String sanitizedQuery = null;

        if (rawQuery != null && !rawQuery.isEmpty()) {
            List<String> remainingParams = new ArrayList<>();
            for (String param : rawQuery.split("&")) {
                String[] pair = param.split("=", 2);
                String key = pair[0];
                String value = pair.length > 1 ? pair[1] : "";

                if (CREDENTIAL_ID_PARAM.equalsIgnoreCase(key) || FALLBACK_CREDENTIAL_ID_PARAM.equalsIgnoreCase(key)) {
                    credentialId = value;
                } else {
                    remainingParams.add(param);
                }
            }
            if (!remainingParams.isEmpty()) {
                sanitizedQuery = String.join("&", remainingParams);
            }
        }

        URI requestUri;
        try {
            requestUri = new URI(
                    originalUri.getScheme(),
                    originalUri.getUserInfo(),
                    originalUri.getHost(),
                    originalUri.getPort(),
                    originalUri.getPath(),
                    sanitizedQuery,
                    originalUri.getFragment());
        } catch (URISyntaxException e) {
            throw new IOException("Failed to sanitize URI: " + location, e);
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

        if (credentialId != null && !credentialId.isEmpty()) {
            if (credentials == null) {
                throw new IOException("Credential ID specified in URL, but no credential resolver was provided.");
            }

            FetchAuthData.UsernamePassword userPass =
                    credentials.get(credentialId, FetchAuthData.UsernamePassword.class);
            if (userPass != null) {
                String authString = userPass.getUsername() + ":" + userPass.getPassword();
                String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));
                requestBuilder.header("Authorization", "Basic " + encodedAuth);
            } else {
                FetchAuthData.Token token = credentials.get(credentialId, FetchAuthData.Token.class);
                if (token != null) {
                    requestBuilder.header("Authorization", "Bearer " + token.getToken());
                } else {
                    throw new IOException(
                            "Unable to resolve credentials with ID '" + credentialId + "' for configuration source.");
                }
            }
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
}
