package org.jenkinsci.plugins.casc.integrations;

import hudson.Extension;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.security.Permission;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.RootElementConfigurator;

import java.util.*;

/**
 * Example
 *
 * jenkins:
 *   authorizationStrategy:
 *     globalMatrix:
 *       grantedPermissions:
 *         - group:
 *             name: "anonymous"
 *             permissions:
 *               - "hudson.model.Hudson.Read"
 *         - group
 *             name: "authenticated"
 *             permissions:
 *               - "hudson.model.Hudson.Administer"
 */
@Extension(optional = true)
public class GlobalMatrixAuthorizationStrategyConfigurator extends Configurator<GlobalMatrixAuthorizationStrategy> implements RootElementConfigurator {

    @Override
    public String getName() {
        return "globalMatrix";
    }

    @Override
    public Class<GlobalMatrixAuthorizationStrategy> getTarget() {
        return GlobalMatrixAuthorizationStrategy.class;
    }

    @Override
    public Set<Attribute> describe() {
        return Collections.singleton(new Attribute<GroupPermissionDefinition>("grantedPermissions", new HashSet<GroupPermissionDefinition>().getClass()));
    }

    @Override
    public GlobalMatrixAuthorizationStrategy configure(Object config) throws Exception {
        Map map = (Map) config;
        Collection o = (Collection<?>)map.get("grantedPermissions");
        Configurator<GroupPermissionDefinition> permissionConfigurator = Configurator.lookup(GroupPermissionDefinition.class);
        Map<Permission,Set<String>> grantedPermissions = new HashMap<>();
        for(Object entry : o) {
            GroupPermissionDefinition gpd = permissionConfigurator.configure(entry);
            //We transform the linear list to a matrix (Where permission is the key instead)
            gpd.grantPermission(grantedPermissions);
        }
        return new GlobalMatrixAuthorizationStrategy(grantedPermissions);
    }
}
