package org.jenkinsci.plugins.casc.settings;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Confogirator settings interface.
 * Defines behavior of {@link org.jenkinsci.plugins.casc.Configurator}s.
 * @see org.jenkinsci.plugins.casc.ConfiguratorContext
 * @see org.jenkinsci.plugins.casc.BaseConfigurator
 */
@Restricted(Beta.class)
public interface ConfiguratorSettings {

    public static final ConfiguratorSettings DEFAULTS = new Defaults();

    public Deprecation getDeprecation();

    public Restriction getRestricted();

    public Unknown getUnknown();

    @org.kohsuke.accmod.Restricted(NoExternalUse.class)
    public static class Defaults implements ConfiguratorSettings {

        @Override
        public Deprecation getDeprecation() {
            return Deprecation.warn;
        }

        @Override
        public Restriction getRestricted() {
            return Restriction.reject;
        }

        @Override
        public Unknown getUnknown() {
            return Unknown.reject;
        }
    }
}
