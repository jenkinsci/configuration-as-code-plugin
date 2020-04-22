package io.jenkins.plugins.casc.impl.configurators.nonnull;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Set;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * This class checks the behaviour of {@link io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator}
 * with {@link Nonnull} {@link Set} parameters
 */
public class NonnullParameterConstructor {
    private Set<String> strings;

    @DataBoundConstructor
    public NonnullParameterConstructor(@NonNull Set<String> strings) {
        this.strings = strings;
    }

    public Set<String> getStrings() {
        return strings;
    }
}
