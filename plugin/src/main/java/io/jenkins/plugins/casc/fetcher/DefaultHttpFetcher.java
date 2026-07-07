package io.jenkins.plugins.casc.fetcher;

import hudson.Extension;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
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

        URLConnection connection = uri.toURL().openConnection();
        InputStream inputStream = connection.getInputStream();

        ResolvedYaml resolved = new ResolvedYaml(fileName, () -> inputStream);

        return new FetchResult(Collections.singletonList(resolved), inputStream);
    }
}
