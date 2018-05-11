package org.jenkinsci.plugins.casc.integrations.globalmatrixauth;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.security.Permission;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.RootElementConfigurator;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author Mads Nielsen
 * @since TODO
 */
@Extension(optional = true)
@Restricted(NoExternalUse.class)
public class GlobalMatrixAuthorizationStrategyConfigurator extends Configurator<GlobalMatrixAuthorizationStrategy> {

    @Override
    public String getName() {
        return "globalMatrix";
    }

    @Override
    public Class<GlobalMatrixAuthorizationStrategy> getTarget() {
        return GlobalMatrixAuthorizationStrategy.class;
    }

    @Override
    @SuppressFBWarnings(value = "DM_NEW_FOR_GETCLASS", justification = "We need a fully qualified type to do proper attribute binding")
    public Set<Attribute> describe() {
        return Collections.singleton(new Attribute<GroupPermissionDefinition>("grantedPermissions", new HashSet<GroupPermissionDefinition>().getClass()));
    }

    @Override
    public GlobalMatrixAuthorizationStrategy configure(Object config) throws ConfiguratorException {
        Map map = (Map) config;
        Collection o = (Collection<?>)map.get("grantedPermissions");
        Configurator<GroupPermissionDefinition> permissionConfigurator = Configurator.lookupOrFail(GroupPermissionDefinition.class);
        Map<Permission,Set<String>> grantedPermissions = new HashMap<>();
        for(Object entry : o) {
            GroupPermissionDefinition gpd = permissionConfigurator.configureNonNull(entry);
            //We transform the linear list to a matrix (Where permission is the key instead)
            gpd.grantPermission(grantedPermissions);
        }

        //TODO: Once change is in place for GlobalMatrixAuthentication. Switch away from reflection
        GlobalMatrixAuthorizationStrategy gms = new GlobalMatrixAuthorizationStrategy();
        try {
            Field f = gms.getClass().getDeclaredField("grantedPermissions");
            f.setAccessible(true);
            f.set(gms, grantedPermissions);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new ConfiguratorException(this, "Cannot set GlobalMatrixAuthorizationStrategy#grantedPermissions via reflection", ex);
        }
        return gms;
    }
}
