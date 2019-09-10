package io.jenkins.plugins.casc.impl.configurators.nonnull.nonnullparampackage;

import hudson.util.Secret;
import javax.annotation.CheckForNull;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Checks the behaviour of {@link io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator} with non null
 * {@link Secret} when using package-level {@link javax.annotation.ParametersAreNonnullByDefault} annotations.
 */
public class PackageParametersNonNullCheckForNull {
    private Secret secret;

    @DataBoundConstructor
    public PackageParametersNonNullCheckForNull(@CheckForNull Secret secret) {
        this.secret = secret;
    }

    public Secret getSecret() {
        return secret;
    }
}
