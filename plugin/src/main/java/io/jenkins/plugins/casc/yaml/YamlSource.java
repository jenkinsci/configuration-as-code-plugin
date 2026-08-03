package io.jenkins.plugins.casc.yaml;

import io.jenkins.plugins.casc.fetcher.ResolvedYaml;
import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class YamlSource<T> {

    public final T source;
    private String name;

    public YamlSource(T source) {
        this.source = source;
    }

    public YamlSource(T source, String name) {
        this.source = source;
        this.name = name;
    }

    public static YamlSource<InputStream> of(InputStream in) {
        return new YamlSource<>(in);
    }

    public static YamlSource<InputStream> of(InputStream in, String name) {
        return new YamlSource<>(in, name);
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

    public static YamlSource<Path> of(Path path, String name) {
        return new YamlSource<>(path, name);
    }

    public static YamlSource<ResolvedYaml> of(ResolvedYaml resolvedYaml) {
        return new YamlSource<>(resolvedYaml, resolvedYaml.relativePath());
    }

    public String source() {
        if (name != null) {
            return name;
        }
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
