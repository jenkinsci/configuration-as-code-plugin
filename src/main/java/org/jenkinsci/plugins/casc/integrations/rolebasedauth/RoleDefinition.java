package org.jenkinsci.plugins.casc.integrations.rolebasedauth;

import com.michelin.cio.hudson.plugins.rolestrategy.Role;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

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

    private final Role role;
    private final Set<String> assignments;

    @DataBoundConstructor
    public RoleDefinition(Role role, Collection<String> assignments) {
        this.role = role;
        this.assignments = assignments != null ? new HashSet<>(assignments) : Collections.emptySet();
    }

    public Role getRole() {
        return role;
    }

    public Set<String> getAssignments() {
        return Collections.unmodifiableSet(assignments);
    }
}
