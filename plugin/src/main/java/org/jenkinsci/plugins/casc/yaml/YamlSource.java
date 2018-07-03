package org.jenkinsci.plugins.casc.yaml;

import java.io.IOException;
import java.io.Reader;

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

    public Reader read() throws IOException {
        return reader.open(source);
    }

    public String source() {
        return source.toString();
    }
}
