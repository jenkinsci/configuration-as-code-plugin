package org.jenkinsci.plugins.casc.core;

import hudson.Extension;
import hudson.security.SecurityRealm;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.model.CNode;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class NoneSecurityRealmConfigurator extends Configurator<SecurityRealm> {

    @Override
    public Class<SecurityRealm> getTarget() {
        return SecurityRealm.class;
    }

    @Override
    public boolean match(Class clazz) {
        // We can't explicitly reference private class SecurityRealm.None
        return clazz.getName().equals("hudson.security.SecurityRealm$None");
    }

    @Nonnull
    @Override
    public SecurityRealm configure(CNode config) throws ConfiguratorException {
        return SecurityRealm.NO_AUTHENTICATION;
    }

    @CheckForNull
    @Override
    public CNode describe(SecurityRealm instance) throws Exception {
        return null;
    }

    @Nonnull
    @Override
    public Set<Attribute> describe() {
        return Collections.emptySet();
    }
}
