package io.jenkins.plugins.casc.core;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.security.HudsonPrivateSecurityRealm;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.impl.attributes.MultivaluedAttribute;
import io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
@Restricted(NoExternalUse.class)
public class HudsonPrivateSecurityRealmConfigurator extends DataBoundConfigurator<HudsonPrivateSecurityRealm> {
    private static final Logger logger = Logger.getLogger(HudsonPrivateSecurityRealmConfigurator.class.getName());
    /// matches HudsonPrivateSecurityRealm.JBCRYPT_HEADER
    private static final String HASHED_PASSWORD_PREFIX = "#jbcrypt:";

    public HudsonPrivateSecurityRealmConfigurator() {
        super(HudsonPrivateSecurityRealm.class);
    }

    @NonNull
    @Override
    public Set<Attribute<HudsonPrivateSecurityRealm, ?>> describe() {
        final Set<Attribute<HudsonPrivateSecurityRealm, ?>> describe = super.describe();
        describe.add(new MultivaluedAttribute<HudsonPrivateSecurityRealm, UserWithPassword>("users", UserWithPassword.class)
            .getter(HudsonPrivateSecurityRealmConfigurator::getter)
            .setter(HudsonPrivateSecurityRealmConfigurator::setter)
        );
        return describe;
    }

    @CheckForNull
    @Override
    public CNode describe(HudsonPrivateSecurityRealm instance, ConfigurationContext context) {
        return null;
    }

    private static Collection<UserWithPassword> getter(HudsonPrivateSecurityRealm target) {
        return target.getAllUsers().stream()
                .map(u -> new UserWithPassword(u.getId(), null)) // password isn't actually stored, only hashed
                .collect(Collectors.toList());
    }

    private static void setter(HudsonPrivateSecurityRealm target, Collection<UserWithPassword> value) throws IOException {
        for (UserWithPassword user : value) {
            if (StringUtils.startsWith(user.password, HASHED_PASSWORD_PREFIX)) {
                try {
                    target.createAccountWithHashedPassword(user.id, user.password);
                } catch (IllegalArgumentException e) {
                    logger.log(Level.WARNING, "Failed to create user with presumed hashed password", e);
                    // fallback, just create the account as is
                    target.createAccount(user.id, user.password);
                }
            } else {
                target.createAccount(user.id, user.password);
            }
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
