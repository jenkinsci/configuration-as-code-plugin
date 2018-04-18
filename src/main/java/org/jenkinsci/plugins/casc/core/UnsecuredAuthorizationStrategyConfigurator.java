package org.jenkinsci.plugins.casc.core;

import hudson.Extension;
import hudson.security.AuthorizationStrategy;
import hudson.security.AuthorizationStrategy.Unsecured;
import org.jenkinsci.plugins.casc.BaseConfigurator;
import org.jenkinsci.plugins.casc.ConfiguratorException;

/**
 * Handles {@link AuthorizationStrategy.Unsecured} that requires a special treatment due to its singleton semantics.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class UnsecuredAuthorizationStrategyConfigurator extends BaseConfigurator<Unsecured> {
    @Override
    public Class<Unsecured> getTarget() {
        return Unsecured.class;
    }

    @Override
    public Unsecured configure(Object config) throws ConfiguratorException {
        return (Unsecured)AuthorizationStrategy.UNSECURED;
    }
}
