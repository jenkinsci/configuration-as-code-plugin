package org.jenkinsci.plugins.casc.integrations.projectmatriaxauth;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.security.Permission;
import hudson.security.ProjectMatrixAuthorizationStrategy;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.integrations.globalmatrixauth.GroupPermissionDefinition;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.jenkinsci.plugins.casc.model.Sequence;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Extension(optional = true)
@Restricted(NoExternalUse.class)
public class ProjectMatrixAuthorizationStrategyConfigurator extends Configurator<ProjectMatrixAuthorizationStrategy> {

    @Override
    public String getName() {
        return "projectMatrix";
    }

    @Override
    public Class<ProjectMatrixAuthorizationStrategy> getTarget() {
        return ProjectMatrixAuthorizationStrategy.class;
    }

    @Override
    public ProjectMatrixAuthorizationStrategy configure(CNode config) throws ConfiguratorException {
        Mapping map = config.asMapping();
        Sequence o = map.get("grantedPermissions").asSequence();
        Configurator<GroupPermissionDefinition> permissionConfigurator = Configurator.lookupOrFail(GroupPermissionDefinition.class);
        Map<Permission,Set<String>> grantedPermissions = new HashMap<>();
        for(CNode entry : o) {
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
    public ProjectMatrixAuthorizationStrategy check(CNode config) throws ConfiguratorException {
        // FIXME
        return null;
    }

    @Override
    @SuppressFBWarnings(value = "DM_NEW_FOR_GETCLASS", justification = "We need a fully qualified type to do proper attribute binding")
    public Set<Attribute> describe() {
        return Collections.singleton(new Attribute<ProjectMatrixAuthorizationStrategy, GroupPermissionDefinition>("grantedPermissions", new HashSet<GroupPermissionDefinition>().getClass()));
    }

    @CheckForNull
    @Override
    public CNode describe(ProjectMatrixAuthorizationStrategy instance) {
        // FIXME
        return null;
    }


}
