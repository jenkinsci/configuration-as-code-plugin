package io.jenkins.plugins.casc.support.matrixauth;

import hudson.security.AuthorizationStrategy;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.security.Permission;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.impl.attributes.MultivaluedAttribute;
import io.jenkins.plugins.casc.util.PermissionFinder;
import org.jenkinsci.plugins.matrixauth.AuthorizationContainer;

import javax.annotation.CheckForNull;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public abstract class MatrixAuthorizationStrategyConfigurator<T> extends BaseConfigurator<T> {

    @CheckForNull
    @Override
    public Class getExtensionPoint() {
        return AuthorizationStrategy.class;
    }


    @Override
    public Set<Attribute> describe() {
        return Collections.singleton(
                new MultivaluedAttribute<GlobalMatrixAuthorizationStrategy, String>("grantedPermissions", String.class)
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
