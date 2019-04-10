package io.jenkins.plugins.casc.yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.servlet.http.HttpServletRequest;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */

public class YamlSource<T> {

    public final T source;

    public final YamlReader<T> reader;

    public YamlSource(T source, YamlReader<T> reader) {
        this.source = source;
        this.reader = reader;
    }

    public static YamlSource<InputStream> of(InputStream in) {
        return new YamlSource<>(in, READ_FROM_INPUTSTREAM);
    }

    public static YamlSource<String> of(URL url) {
        return new YamlSource<>(url.toExternalForm(), READ_FROM_URL);
    }

    public static YamlSource<String> of(String url) {
        return new YamlSource<>(url, READ_FROM_URL);
    }

    public static YamlSource<HttpServletRequest> of(HttpServletRequest req) {
        return new YamlSource<>(req, YamlSource.READ_FROM_REQUEST);
    }

    public static YamlSource<Path> of(Path path) {
        return new YamlSource<>(path, YamlSource.READ_FROM_PATH);
    }

    public Reader read() throws IOException {
        return reader.open(source);
    }

    public String source() {
        return source.toString();
    }

    public static final YamlReader<String> READ_FROM_URL = config -> {
        final URL url = URI.create(config).toURL();
        return new InputStreamReader(url.openStream(), UTF_8);
    };

    public static final YamlReader<Path> READ_FROM_PATH = Files::newBufferedReader;

    public static final YamlReader<InputStream> READ_FROM_INPUTSTREAM = in -> new InputStreamReader(in, UTF_8);

    public static final YamlReader<HttpServletRequest> READ_FROM_REQUEST = req -> {
        // TODO get encoding from req.getContentType()
        return new InputStreamReader(req.getInputStream(), UTF_8);
    };

    @Override
    public String toString() {
        return "YamlSource: " + source;
    }
}
