package io.jenkins.plugins.casc.core;

import hudson.Extension;
import hudson.security.AuthorizationStrategy;
import hudson.security.AuthorizationStrategy.Unsecured;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;

/**
 * Handles {@link AuthorizationStrategy.Unsecured} that requires a special treatment due to its singleton semantics.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
@Restricted(NoExternalUse.class)
public class UnsecuredAuthorizationStrategyConfigurator extends BaseConfigurator<Unsecured> {
    @Override
    public Class<Unsecured> getTarget() {
        return Unsecured.class;
    }

    @Override
    protected Unsecured instance(Mapping mapping, ConfigurationContext context) {
        return (Unsecured)AuthorizationStrategy.UNSECURED;
    }

    @CheckForNull
    @Override
    public CNode describe(Unsecured instance, ConfigurationContext context) {
        return null; // FIXME
    }
}
