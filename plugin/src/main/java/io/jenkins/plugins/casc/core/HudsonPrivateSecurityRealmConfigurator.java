package io.jenkins.plugins.casc.core;

import hudson.Extension;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.security.HudsonPrivateSecurityRealm;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.impl.attributes.MultivaluedAttribute;
import io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
@Restricted(NoExternalUse.class)
public class HudsonPrivateSecurityRealmConfigurator extends DataBoundConfigurator {
    /// matches HudsonPrivateSecurityRealm.JBCRYPT_HEADER
    private static final String HASHED_PASSWORD_PREFIX = "#jbcrypt:";

    public HudsonPrivateSecurityRealmConfigurator() {
        super(HudsonPrivateSecurityRealm.class);
    }

    @Override
    public Set<Attribute> describe() {
        final Set<Attribute> describe = super.describe();
        describe.add(new MultivaluedAttribute<HudsonPrivateSecurityRealm, UserWithPassword>("users", UserWithPassword.class)
            .getter(target ->
                target.getAllUsers().stream()
                    .map(u -> new UserWithPassword(u.getId(), null))    // password isn't actually stored, only hashed
                    .collect(Collectors.toList()))
            .setter((target, value) -> {
                for (UserWithPassword user : value) {
                    if (user.password.startsWith(HASHED_PASSWORD_PREFIX)) {
                        User jenkinsUser = User.getById(user.id, true);
                        jenkinsUser.addProperty(getHashedPassword(user.password));
                    } else {
                        target.createAccount(user.id, user.password);
                    }
                }
            }
        ));
        return describe;
    }

    private static UserProperty getHashedPassword(String hashedPassword) {
        try {
            Method fromHashedPassword = HudsonPrivateSecurityRealm.Details.class.getDeclaredMethod("fromHashedPassword", String.class);
            fromHashedPassword.setAccessible(true);
            return (UserProperty) fromHashedPassword.invoke(null, hashedPassword);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Failed to construct hashed password", e.getCause());
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Failed to construct hashed password", e);
        }
    }

    public static class UserWithPassword {
        private final String id;
        private final String password;

        @DataBoundConstructor
        public UserWithPassword(String id, String password) {
            this.id = id;
            this.password = password;
        }

    }
}
