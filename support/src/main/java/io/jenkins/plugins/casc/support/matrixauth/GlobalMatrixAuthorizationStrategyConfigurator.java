package io.jenkins.plugins.casc.support.matrixauth;

import hudson.Extension;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;

/**
 * @author Mads Nielsen
 * @since TODO
 */
@Extension(optional = true, ordinal = 1)
@Restricted(NoExternalUse.class)
public class GlobalMatrixAuthorizationStrategyConfigurator extends MatrixAuthorizationStrategyConfigurator<GlobalMatrixAuthorizationStrategy> {

    @Override
    public String getName() {
        return "globalMatrix";
    }

    @Override
    public Class<GlobalMatrixAuthorizationStrategy> getTarget() {
        return GlobalMatrixAuthorizationStrategy.class;
    }

    @Override
    public GlobalMatrixAuthorizationStrategy instance(Mapping mapping, ConfigurationContext context) throws ConfiguratorException {
        return new GlobalMatrixAuthorizationStrategy();
    }

    @CheckForNull
    @Override
    public CNode describe(GlobalMatrixAuthorizationStrategy instance, ConfigurationContext context) throws Exception {
        return compare(instance, new GlobalMatrixAuthorizationStrategy(), context);
    }
}
