package io.jenkins.plugins.casc.support.rolestrategy;


import com.michelin.cio.hudson.plugins.rolestrategy.Role;
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Provides the configuration logic for Role Strategy plugin.
 * @author Oleg Nenashev
 * @since TODO
 */
@Extension(optional = true)
@Restricted({NoExternalUse.class})
public class RoleBasedAuthorizationStrategyConfigurator extends BaseConfigurator<RoleBasedAuthorizationStrategy> {

    @Override
    @NonNull
    public String getName() {
        return "roleStrategy";
    }

    @Override
    public Class<RoleBasedAuthorizationStrategy> getTarget() {
        return RoleBasedAuthorizationStrategy.class;
    }

    @NonNull
    @Override
    public Class getImplementedAPI() {
        return GrantedRoles.class;
    }

    @Override
    protected RoleBasedAuthorizationStrategy instance(Mapping map, ConfigurationContext context) throws ConfiguratorException {
        final Configurator<GrantedRoles> c = context.lookupOrFail(GrantedRoles.class);
        final GrantedRoles roles = c.configure(map.remove("roles"), context);
        return new RoleBasedAuthorizationStrategy(roles.toMap());
    }

    @Override
    @NonNull
    public Set<Attribute<RoleBasedAuthorizationStrategy,?>> describe() {
        return Collections.singleton(
                new Attribute<RoleBasedAuthorizationStrategy, GrantedRoles>("roles", GrantedRoles.class).getter(target -> {
                    List<RoleDefinition> globalRoles = getRoleDefinitions(target.getGrantedRoles(RoleBasedAuthorizationStrategy.GLOBAL));
                    List<RoleDefinition> agentRoles = getRoleDefinitions(target.getGrantedRoles(RoleBasedAuthorizationStrategy.SLAVE));
                    List<RoleDefinition> projectRoles = getRoleDefinitions(target.getGrantedRoles(RoleBasedAuthorizationStrategy.PROJECT));
                    return new GrantedRoles(globalRoles, projectRoles, agentRoles);
                }));
    }

    private List<RoleDefinition> getRoleDefinitions(@CheckForNull SortedMap<Role, Set<String>> roleMap) {
        if (roleMap == null) return Collections.emptyList();
        return roleMap.entrySet().stream().map(getRoleDefinition()).collect(Collectors.toList());
    }

    private Function<Entry<Role, Set<String>>, RoleDefinition> getRoleDefinition() {
        return roleSetEntry -> {
            Role role = roleSetEntry.getKey();
            List<String> permissions = role.getPermissions().stream()
                    .map(permission -> permission.group.title.toString(
                            Locale.US) + "/" + permission.name).collect(Collectors.toList());
            return new RoleDefinition(role.getName(), role.getDescription(),
                    role.getPattern().pattern(), permissions, roleSetEntry.getValue());
        };
    }


    @CheckForNull
    @Override
    public CNode describe(RoleBasedAuthorizationStrategy instance, ConfigurationContext context) throws Exception {
        return compare(instance, new RoleBasedAuthorizationStrategy(Collections.emptyMap()), context);
    }

}
