package io.jenkins.plugins.casc.support.matrixauth;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.security.AuthorizationStrategy;
import hudson.security.Permission;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.impl.attributes.MultivaluedAttribute;
import io.jenkins.plugins.casc.util.PermissionFinder;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.jenkinsci.plugins.matrixauth.AuthorizationContainer;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public abstract class MatrixAuthorizationStrategyConfigurator<T extends AuthorizationContainer> extends BaseConfigurator<T> {

    @NonNull
    @Override
    public Class getImplementedAPI() {
        return AuthorizationStrategy.class;
    }


    @Override
    @NonNull
    public Set<Attribute<T, ?>> describe() {
        return Collections.singleton(
                new MultivaluedAttribute<T, String>("grantedPermissions", String.class)
                        .getter(MatrixAuthorizationStrategyConfigurator::getGrantedPermissions)
                        .setter(MatrixAuthorizationStrategyConfigurator::setGrantedPermissions)
        );
    }

    /**
     * Extract container's permissions as a List of "PERMISSION:sid"
     */
    static Collection<String> getGrantedPermissions(AuthorizationContainer container) {
        return container.getGrantedPermissions().entrySet().stream()
                .flatMap( e -> e.getValue().stream().map(v -> e.getKey()+":"+v))
                .collect(Collectors.toList());
    }

    /**
     * Configure container's permissions from a List of "PERMISSION:sid"
     * @param container
     * @param permissions
     */
    static void setGrantedPermissions(AuthorizationContainer container, Collection<String> permissions) {
        permissions.forEach(p -> {
            final int i = p.indexOf(':');
            final Permission permission = PermissionFinder.findPermission(p.substring(0, i));
            container.add(permission, p.substring(i+1));
        });
    }
}
