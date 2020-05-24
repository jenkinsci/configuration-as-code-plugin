package io.jenkins.plugins.casc.yaml;

import java.io.InputStream;
import java.nio.file.Path;
import javax.servlet.http.HttpServletRequest;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */

public class YamlSource<T> {

    public final T source;

    public YamlSource(T source) {
        this.source = source;
    }

    public static YamlSource<InputStream> of(InputStream in) {
        return new YamlSource<>(in);
    }

    public static YamlSource<String> of(String url) {
        return new YamlSource<>(url);
    }

    public static YamlSource<HttpServletRequest> of(HttpServletRequest req) {
        return new YamlSource<>(req);
    }

    public static YamlSource<Path> of(Path path) {
        return new YamlSource<>(path);
    }

    public String source() {
        if (source instanceof HttpServletRequest) {
            return ((HttpServletRequest) source).getPathInfo();
        }
        return source.toString();
    }

    @Override
    public String toString() {
        return "YamlSource: " + source();
    }
}
