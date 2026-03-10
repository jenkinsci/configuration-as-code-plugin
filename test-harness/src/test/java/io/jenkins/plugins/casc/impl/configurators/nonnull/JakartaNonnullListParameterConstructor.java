package io.jenkins.plugins.casc.impl.configurators.nonnull;

import jakarta.annotation.Nonnull;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

public class JakartaNonnullListParameterConstructor {
    private List<String> strings;

    @DataBoundConstructor
    public JakartaNonnullListParameterConstructor(@Nonnull List<String> strings) {
        this.strings = strings;
    }

    public List<String> getStrings() {
        return strings;
    }
}
