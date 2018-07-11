package org.jenkinsci.plugins.casc.yaml;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;

import java.io.IOException;
import java.io.Reader;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@FunctionalInterface
@Restricted(Beta.class)
public interface YamlReader<T> {

    Reader open(T source) throws IOException;
}
