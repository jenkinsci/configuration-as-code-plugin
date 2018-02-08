package org.jenkinsci.plugins.casc.integrations.globalmatrixauth;

import hudson.security.Permission;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by mads on 2/7/18.
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
            Permission pm = Permission.fromId(permission);
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
