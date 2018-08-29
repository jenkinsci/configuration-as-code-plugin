package io.jenkins.plugins.casc.support.rolestrategy;


import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy;
import hudson.Extension;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import java.util.Collections;
import java.util.Set;

/**
 * Provides the configuration logic for Role Strategy plugin.
 * @author Oleg Nenashev
 * @since TODO
 */
@Extension(optional = true)
@Restricted({NoExternalUse.class})
public class RoleBasedAuthorizationStrategyConfigurator extends BaseConfigurator<RoleBasedAuthorizationStrategy> {

    @Override
    public String getName() {
        return "roleStrategy";
    }

    @Override
    public Class<RoleBasedAuthorizationStrategy> getTarget() {
        return RoleBasedAuthorizationStrategy.class;
    }

    @Override
    protected RoleBasedAuthorizationStrategy instance(Mapping map, ConfigurationContext context) throws ConfiguratorException {
        final Configurator<GrantedRoles> c = context.lookupOrFail(GrantedRoles.class);
        final GrantedRoles roles = c.configure(map.remove("roles"), context);
        return new RoleBasedAuthorizationStrategy(roles.toMap());
    }

    @Override
    public Set<Attribute<RoleBasedAuthorizationStrategy,?>> describe() {
        return Collections.singleton(
                new Attribute<RoleBasedAuthorizationStrategy, GrantedRoles>("roles", GrantedRoles.class));
    }

    @CheckForNull
    @Override
    public CNode describe(RoleBasedAuthorizationStrategy instance, ConfigurationContext context) throws Exception {
        return compare(instance, new RoleBasedAuthorizationStrategy(Collections.emptyMap()), context);
    }


}
