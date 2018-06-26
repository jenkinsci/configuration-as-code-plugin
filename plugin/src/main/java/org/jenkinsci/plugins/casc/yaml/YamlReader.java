package org.jenkinsci.plugins.casc.yaml;

import java.io.IOException;
import java.io.Reader;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@FunctionalInterface
public interface YamlReader<T> {

    Reader open(T source) throws IOException;
}
