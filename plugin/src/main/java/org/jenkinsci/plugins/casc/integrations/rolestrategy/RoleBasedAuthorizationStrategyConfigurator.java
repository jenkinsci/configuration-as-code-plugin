package org.jenkinsci.plugins.casc.integrations.rolestrategy;


import com.michelin.cio.hudson.plugins.rolestrategy.Role;
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy;
import com.michelin.cio.hudson.plugins.rolestrategy.RoleMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.ConfigurationContext;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.impl.attributes.MultivaluedAttribute;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Provides the configuration logic for Role Strategy plugin.
 * @author Oleg Nenashev
 * @since TODO
 */
@Extension(optional = true)
@Restricted({NoExternalUse.class})
public class RoleBasedAuthorizationStrategyConfigurator extends Configurator<RoleBasedAuthorizationStrategy> {

    @Override
    public String getName() {
        return "roleStrategy";
    }

    @Override
    public Class<RoleBasedAuthorizationStrategy> getTarget() {
        return RoleBasedAuthorizationStrategy.class;
    }

    @Override
    public RoleBasedAuthorizationStrategy configure(CNode config, ConfigurationContext context) throws ConfiguratorException {
        //TODO: API should return a qualified type
        final Configurator<RoleDefinition> roleDefinitionConfigurator =
                (Configurator<RoleDefinition>) context.lookupOrFail(RoleDefinition.class);

        Mapping map = config.asMapping();
        Map<String, RoleMap> grantedRoles = new HashMap<>();

        CNode rolesConfig = map.get("roles");
        if (rolesConfig != null) {
            grantedRoles.put(RoleBasedAuthorizationStrategy.GLOBAL,
                    retrieveRoleMap(rolesConfig, "global", roleDefinitionConfigurator, context));
            grantedRoles.put(RoleBasedAuthorizationStrategy.PROJECT,
                    retrieveRoleMap(rolesConfig, "items", roleDefinitionConfigurator, context));
            grantedRoles.put(RoleBasedAuthorizationStrategy.SLAVE,
                    retrieveRoleMap(rolesConfig, "agents", roleDefinitionConfigurator, context));
        }
        return new RoleBasedAuthorizationStrategy(grantedRoles);
    }

    @Override
    public RoleBasedAuthorizationStrategy check(CNode config, ConfigurationContext context) {
        // FIXME
        return null;
    }

    @Nonnull
    private static RoleMap retrieveRoleMap(@Nonnull CNode config, @Nonnull String name, Configurator<RoleDefinition> configurator, ConfigurationContext context) throws ConfiguratorException {
        Mapping map = config.asMapping();
        final CNode c = map.get(name);

        TreeMap<Role, Set<String>> resMap = new TreeMap<>();
        if (c == null || c.asSequence() == null) {
            // we cannot return emptyMap here due to the Role Strategy code
            return new RoleMap(resMap);
        }

        for (CNode entry : c.asSequence()) {
            RoleDefinition definition = configurator.configure(entry, context);
            resMap.put(definition.getRole(), definition.getAssignments());
        }

        return new RoleMap(resMap);
    }

    @Override
    @SuppressFBWarnings(value = "DM_NEW_FOR_GETCLASS", justification = "We need a fully qualified type to do proper attribute binding")
    public Set<Attribute> describe() {
        return new HashSet<>(Arrays.asList(
                new MultivaluedAttribute<RoleBasedAuthorizationStrategy, RoleDefinition>("global", RoleDefinition.class),
                new MultivaluedAttribute<RoleBasedAuthorizationStrategy, RoleDefinition>("items", RoleDefinition.class),
                new MultivaluedAttribute<RoleBasedAuthorizationStrategy, RoleDefinition>("agents", RoleDefinition.class)
        ));
    }

    @CheckForNull
    @Override
    public CNode describe(RoleBasedAuthorizationStrategy instance, ConfigurationContext context) {
        // FIXME
        return null;
    }


}
