package io.jenkins.plugins.casc.core;

import hudson.Extension;
import hudson.security.SecurityRealm;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.CNode;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
@Restricted(NoExternalUse.class)
public class NoneSecurityRealmConfigurator implements Configurator<SecurityRealm> {

    @Override
    public Class<SecurityRealm> getTarget() {
        return SecurityRealm.class;
    }

    @Override
    public boolean canConfigure(Class clazz) {
        // We can't explicitly reference private class SecurityRealm.None
        return clazz.getName().equals("hudson.security.SecurityRealm$None");
    }

    @Nonnull
    @Override
    public SecurityRealm configure(CNode config, ConfigurationContext context) throws ConfiguratorException {
        return SecurityRealm.NO_AUTHENTICATION;
    }

    @Override
    public SecurityRealm check(CNode config, ConfigurationContext context) {
        return SecurityRealm.NO_AUTHENTICATION;
    }

    @CheckForNull
    @Override
    public CNode describe(SecurityRealm instance, ConfigurationContext context) throws Exception {
        return null;
    }

    @Nonnull
    @Override
    public Set<Attribute<SecurityRealm,?>> describe() {
        return Collections.emptySet();
    }
}
