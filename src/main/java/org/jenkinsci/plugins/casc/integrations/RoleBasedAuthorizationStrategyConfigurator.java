package org.jenkinsci.plugins.casc.integrations;


import com.cloudbees.plugins.credentials.domains.Domain;
import com.michelin.cio.hudson.plugins.rolestrategy.Role;
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy;
import com.michelin.cio.hudson.plugins.rolestrategy.RoleMap;
import hudson.Extension;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.RootElementConfigurator;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Provides the configuration logic for Role Strategy plugin.
 * @author Oleg Nenashev
 * @since TODO
 */
@Extension(optional = true)
@Restricted({NoExternalUse.class})
public class RoleBasedAuthorizationStrategyConfigurator extends Configurator<RoleBasedAuthorizationStrategy> implements RootElementConfigurator {

    @Override
    public String getName() {
        return "roleStrategy";
    }

    @Override
    public Class<RoleBasedAuthorizationStrategy> getTarget() {
        return RoleBasedAuthorizationStrategy.class;
    }

    @Override
    public RoleBasedAuthorizationStrategy configure(Object config) throws Exception {
        //TODO: API should return a qualified type
        final Configurator<RoleDefinition> roleDefinitionConfigurator =
                (Configurator<RoleDefinition>) Configurator.lookup(RoleDefinition.class);
        if (roleDefinitionConfigurator == null) {
            throw new IOException("Cannot find configurator for" + RoleDefinition.class);
        }


        Map map = (Map) config;
        Map<String, RoleMap> grantedRoles = new HashMap<>();

        Object rolesConfig = map.get("roles");
        if (rolesConfig != null) {
            grantedRoles.put(RoleBasedAuthorizationStrategy.GLOBAL,
                    retrieveRoleMap(rolesConfig, "global", roleDefinitionConfigurator));
            grantedRoles.put(RoleBasedAuthorizationStrategy.PROJECT,
                    retrieveRoleMap(rolesConfig, "items", roleDefinitionConfigurator));
            grantedRoles.put(RoleBasedAuthorizationStrategy.SLAVE,
                    retrieveRoleMap(rolesConfig, "agents", roleDefinitionConfigurator));
        }
        return new RoleBasedAuthorizationStrategy(grantedRoles);
    }

    @Nonnull
    private static RoleMap retrieveRoleMap(@Nonnull Object config, @Nonnull String name, Configurator<RoleDefinition> configurator) throws Exception {
        Map map = (Map) config;
        final Collection<?> c = (Collection<?>) map.get(name);

        TreeMap<Role, Set<String>> resMap = new TreeMap<>();
        if (c == null) {
            // we cannot return emptyMap here due to the Role Strategy code
            return new RoleMap(resMap);
        }

        for (Object entry : c) {
            RoleDefinition definition = configurator.configure(entry);
            resMap.put(definition.getRole(), definition.getAssignments());
        }

        return new RoleMap(resMap);
    }

    @Override
    public Set<Attribute> describe() {
        Class<?> groupType = new HashSet<RoleDefinition>().getClass();
        return new HashSet<>(Arrays.asList(
                new Attribute("global", groupType),
                new Attribute("items", groupType),
                new Attribute("agents", groupType)
        ));
    }


}
