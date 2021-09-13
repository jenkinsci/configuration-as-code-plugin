package io.jenkins.plugins.casc.yaml;

import java.io.Reader;
import java.lang.reflect.Field;
import org.yaml.snakeyaml.reader.StreamReader;

/**
 * Hack StreamReader to track the source file/url configuration node have been parsed from
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
class StreamReaderWithSource extends StreamReader {

    public StreamReaderWithSource(YamlSource source, Reader reader) {
        super(reader);
        try {
            final Field f = StreamReader.class.getDeclaredField("name");
            f.setAccessible(true);
            f.set(this, source.source());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Can't track origin, maybe due to SecurityManager ?
            // never mind
        }
    }
}
