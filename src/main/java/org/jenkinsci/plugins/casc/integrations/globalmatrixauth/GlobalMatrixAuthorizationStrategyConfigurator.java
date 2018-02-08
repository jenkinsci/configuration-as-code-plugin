package org.jenkinsci.plugins.casc.integrations.globalmatrixauth;

import hudson.Extension;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.RootElementConfigurator;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.util.*;

@Extension(optional = true)
@Restricted(NoExternalUse.class)
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
