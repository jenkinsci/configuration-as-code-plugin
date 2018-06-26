package org.jenkinsci.plugins.casc.integrations.globalmatrixauth;

import hudson.security.Permission;
import org.jenkinsci.plugins.casc.util.PermissionFinder;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author Mads Nielsen
 * @since TODO
 */
@Restricted(NoExternalUse.class)
public class GroupPermissionDefinition {

    private String name;
    private Collection<String> permissions;

    private static final Logger LOGGER = Logger.getLogger(GroupPermissionDefinition.class.getName());

    @DataBoundConstructor
    public GroupPermissionDefinition(String name, Collection<String> permissions) {
        this.name = name;
        this.permissions = permissions != null ? Collections.unmodifiableCollection(permissions) : Collections.EMPTY_SET;
    }

    public void grantPermission(Map<Permission,Set<String>> grantedPermissions) {

        for(String permission : permissions) {
            //Permission pm = Permission.fromId(permission);
            Permission pm = PermissionFinder.findPermission(permission);
            if(pm != null) {
                if (grantedPermissions.containsKey(pm)) {
                    grantedPermissions.get(pm).add(name);
                } else {
                    HashSet<String> s = new HashSet<>();
                    s.add(name);
                    grantedPermissions.put(pm, s);
                }
            } else {
                LOGGER.warning(String.format("Ignoring unknown permission with id: '%s'", permission));
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("'%s' granted [%s]", name, permissions != null ? String.join(",", permissions) : "" );
    }
}
