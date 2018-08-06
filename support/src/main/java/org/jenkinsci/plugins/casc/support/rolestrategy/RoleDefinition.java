package org.jenkinsci.plugins.casc.support.rolestrategy;

import com.michelin.cio.hudson.plugins.rolestrategy.Role;
import org.jenkinsci.plugins.casc.util.PermissionFinder;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Role definition.
 * Used for custom formatting
 * @author Oleg Nenashev
 * @since TODO
 */
@Restricted(NoExternalUse.class)
public class RoleDefinition {

    private transient Role role;

    @Nonnull
    private final String name;
    @CheckForNull
    private final String description;
    @CheckForNull
    private final String pattern;
    private final Set<String> permissions;
    private final Set<String> assignments;

    @DataBoundConstructor
    public RoleDefinition(String name, String description, String pattern, Collection<String> permissions, Collection<String> assignments) {
        this.name = name;
        this.description = description;
        this.pattern = pattern;
        this.permissions = permissions != null ? new HashSet<>(permissions) : Collections.emptySet();
        this.assignments = assignments != null ? new HashSet<>(assignments) : Collections.emptySet();
        this.role = getRole();
    }

    public final Role getRole() {
        if (role == null) {
            HashSet<String> resolvedIds = new HashSet<>();
            for (String id : permissions) {
                String resolvedId = PermissionFinder.findPermissionId(id);
                if (resolvedId != null) {
                    resolvedIds.add(resolvedId);
                } else {
                    throw new IllegalStateException("Cannot resolve permission for ID: " + id);
                }
            }
            role = new Role(name, pattern, resolvedIds, description);
        }
        return role;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPattern() {
        return pattern;
    }

    public Set<String> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public Set<String> getAssignments() {
        return Collections.unmodifiableSet(assignments);
    }

}
