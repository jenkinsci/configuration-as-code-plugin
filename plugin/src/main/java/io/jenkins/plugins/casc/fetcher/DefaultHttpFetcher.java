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
import java.time.Duration;
import java.util.Collections;

@Extension(ordinal = -100)
public class DefaultHttpFetcher implements CasCConfigFetcher {

    @Override
    public boolean supports(String location) {
        return location != null && (location.startsWith("http://") || location.startsWith("https://"));
    }

    @Override
    public FetchResult fetch(String location, FetchCredentials credentials) throws IOException {
        URI uri;
        try {
            uri = new URI(location);
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URL: " + location, e);
        }

        String path = uri.getPath();
        String fileName =
                (path != null && path.contains("/")) ? path.substring(path.lastIndexOf('/') + 1) : "casc.yaml";

        if (fileName.isEmpty()) {
            fileName = "casc.yaml";
        }

        HttpClient client = ProxyConfiguration.newHttpClient();

        HttpRequest request = ProxyConfiguration.newHttpRequestBuilder(uri)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        byte[] yamlBytes;
        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Failed to fetch configuration from " + location + ". HTTP status code: "
                        + response.statusCode());
            }

            yamlBytes = response.body();
        } catch (InterruptedException e) {
            currentThread().interrupt();
            throw new IOException("Interrupted while fetching configuration from: " + location, e);
        }

        ResolvedYaml resolved = new ResolvedYaml(fileName, () -> new ByteArrayInputStream(yamlBytes));

        return new FetchResult(Collections.singletonList(resolved), (AutoCloseable) null);
    }
}
