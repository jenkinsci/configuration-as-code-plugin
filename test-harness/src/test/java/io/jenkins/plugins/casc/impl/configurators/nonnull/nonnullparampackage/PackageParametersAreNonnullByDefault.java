package io.jenkins.plugins.casc.impl.configurators.nonnull.nonnullparampackage;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Checks the behaviour of {@link io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator} with non null
 * {@link String} when using package-level {@link javax.annotation.ParametersAreNonnullByDefault} annotations.
 */
public class PackageParametersAreNonnullByDefault {
    private String string;

    @DataBoundConstructor
    public PackageParametersAreNonnullByDefault(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }
}
