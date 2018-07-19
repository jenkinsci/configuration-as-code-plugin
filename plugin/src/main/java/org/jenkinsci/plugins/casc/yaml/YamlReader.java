package org.jenkinsci.plugins.casc.yaml;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.None;

import java.io.IOException;
import java.io.Reader;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@FunctionalInterface
@Restricted(None.class /** should be Beta, see #322 */)
public interface YamlReader<T> {

    Reader open(T source) throws IOException;
}
