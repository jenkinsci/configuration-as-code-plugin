package io.jenkins.plugins.casc.impl.configurators.nonnull;

import jakarta.annotation.Nonnull;
import java.util.Set;
import org.kohsuke.stapler.DataBoundConstructor;

public class JakartaNonnullParameterConstructor {
    private Set<String> strings;

    @DataBoundConstructor
    public JakartaNonnullParameterConstructor(@Nonnull Set<String> strings) {
        this.strings = strings;
    }

    public Set<String> getStrings() {
        return strings;
    }
}
