package org.jenkinsci.plugins.casc.integrations;

import hudson.security.Permission;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.*;

/**
 * Created by mads on 2/7/18.
 */
public class GroupPermissionDefinition {

    private String name;
    private Collection<String> permissions = new ArrayList<>();

    @DataBoundConstructor
    public GroupPermissionDefinition(String name, Collection<String> permissions) {
        this.name = name;
        this.permissions = permissions;
    }

    public void grantPermission(Map<Permission,Set<String>> grantedPermissions) {
        for(String permission : permissions) {
            Permission pm = Permission.fromId(permission);
            if(grantedPermissions.containsKey(pm)) {
                grantedPermissions.get(pm).add(name);
            } else {
                HashSet<String> s = new HashSet<>();
                s.add(name);
                grantedPermissions.put(pm, s);
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
