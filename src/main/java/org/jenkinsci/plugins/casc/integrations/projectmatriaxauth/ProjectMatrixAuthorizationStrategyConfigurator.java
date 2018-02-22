package org.jenkinsci.plugins.casc.integrations.projectmatriaxauth;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.security.Permission;
import hudson.security.ProjectMatrixAuthorizationStrategy;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.RootElementConfigurator;
import org.jenkinsci.plugins.casc.integrations.globalmatrixauth.GroupPermissionDefinition;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.util.*;

@Extension(optional = true)
@Restricted(NoExternalUse.class)
public class ProjectMatrixAuthorizationStrategyConfigurator extends Configurator<ProjectMatrixAuthorizationStrategy> implements RootElementConfigurator {

    @Override
    public String getName() {
        return "projectMatrix";
    }

    @Override
    public Class<ProjectMatrixAuthorizationStrategy> getTarget() {
        return ProjectMatrixAuthorizationStrategy.class;
    }

    @Override
    public ProjectMatrixAuthorizationStrategy configure(Object config) throws Exception {
        Map map = (Map) config;
        Collection o = (Collection<?>)map.get("grantedPermissions");
        Configurator<GroupPermissionDefinition> permissionConfigurator = Configurator.lookup(GroupPermissionDefinition.class);
        Map<Permission,Set<String>> grantedPermissions = new HashMap<>();
        for(Object entry : o) {
            GroupPermissionDefinition gpd = permissionConfigurator.configure(entry);
            //We transform the linear list to a matrix (Where permission is the key instead)
            gpd.grantPermission(grantedPermissions);
        }

        ProjectMatrixAuthorizationStrategy gms = new ProjectMatrixAuthorizationStrategy();
        for(Map.Entry<Permission,Set<String>> permission : grantedPermissions.entrySet()) {
            for(String sid : permission.getValue()) {
                gms.add(permission.getKey(), sid);
            }
        }

        return gms;
    }

    @Override
    @SuppressFBWarnings(value = "DM_NEW_FOR_GETCLASS", justification = "We need a fully qualified type to do proper attribute binding")
    public Set<Attribute> describe() {
        return Collections.singleton(new Attribute<GroupPermissionDefinition>("grantedPermissions", new HashSet<GroupPermissionDefinition>().getClass()));
    }

}
