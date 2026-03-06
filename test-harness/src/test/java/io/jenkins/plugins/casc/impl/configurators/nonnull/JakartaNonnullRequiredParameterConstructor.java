package io.jenkins.plugins.casc.impl.configurators.nonnull;

import jakarta.annotation.Nonnull;
import org.kohsuke.stapler.DataBoundConstructor;

public class JakartaNonnullRequiredParameterConstructor {
    private String string;

    @DataBoundConstructor
    public JakartaNonnullRequiredParameterConstructor(@Nonnull String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }
}
