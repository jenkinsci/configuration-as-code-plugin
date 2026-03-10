package io.jenkins.plugins.casc.impl.configurators.nonnull;

import hudson.util.Secret;
import jakarta.annotation.Nullable;
import org.kohsuke.stapler.DataBoundConstructor;

public class JakartaNullableParameterConstructor {
    private Secret secret;

    @DataBoundConstructor
    public JakartaNullableParameterConstructor(@Nullable Secret secret) {
        this.secret = secret;
    }

    public Secret getSecret() {
        return secret;
    }
}
