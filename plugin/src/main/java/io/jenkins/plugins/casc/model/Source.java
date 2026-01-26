package io.jenkins.plugins.casc.model;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.Serial;
import java.io.Serializable;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public record Source(String file, int line) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NonNull
    @Override
    public String toString() {
        return (file != null ? file + ":" : "line ") + line;
    }
}
