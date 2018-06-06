package org.jenkinsci.plugins.casc.model;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class Source {

    public final String file;
    public final int line;

    public Source(String file, int line) {
        this.file = file;
        this.line = line;
    }
}
