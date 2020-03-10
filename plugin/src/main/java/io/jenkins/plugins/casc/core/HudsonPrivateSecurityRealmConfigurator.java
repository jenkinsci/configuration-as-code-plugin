package io.jenkins.plugins.casc.core;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.security.HudsonPrivateSecurityRealm;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.impl.attributes.MultivaluedAttribute;
import io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

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
    public CNode describe(HudsonPrivateSecurityRealm instance, ConfigurationContext context)
        throws Exception {
        // allow disabling exporting users if an instance has too many
        if (System.getProperty("io.jenkins.plugins.casc.core.HudsonPrivateSecurityRealmConfigurator.exportUsers", "true").equals("true")) {
            return super.describe(instance, context);
        }
        return null;
    }

    private static Collection<UserWithPassword> getter(HudsonPrivateSecurityRealm target) {
        return target.getAllUsers().stream()
                .map(u -> {
                    UserWithPassword user = new UserWithPassword(u.getId(), null);
                    user.setName(u.getFullName());
                    user.setDescription(u.getDescription());
                    List<UserProperty> properties = u.getAllProperties()
                        .stream()
                        .filter(userProperty -> !userProperty.getClass().getName().equals("com.cloudbees.plugins.credentials.UserCredentialsProvider$UserCredentialsProperty"))
                        .collect(Collectors.toList());
                    user.setProperties(properties);

                    return user;
                })
                .collect(Collectors.toList());
    }

    private static void setter(HudsonPrivateSecurityRealm target, Collection<UserWithPassword> value) throws IOException {
        for (UserWithPassword user : value) {
            User updatedUser = createAccount(target, user);
            updatedUser.setFullName(user.name);
            updatedUser.setDescription(user.description);
            if (user.getProperties() != null) {
                for (UserProperty property : user.getProperties()) {
                    updatedUser.addProperty(property);
                }
            }
        }
    }

    private static User createAccount(HudsonPrivateSecurityRealm target, UserWithPassword user)
        throws IOException {
        User updatedUser;
        if (StringUtils.isNotBlank(user.password)) {
            if (StringUtils.startsWith(user.password, HASHED_PASSWORD_PREFIX)) {
                try {
                    updatedUser = target
                        .createAccountWithHashedPassword(user.id, user.password);
                } catch (IllegalArgumentException | IOException e) {
                    logger.log(Level.WARNING,
                        "Failed to create user with presumed hashed password", e);
                    // fallback, just create the account as is
                    updatedUser = target.createAccount(user.id, user.password);
                }
            } else {
                updatedUser = target.createAccount(user.id, user.password);
            }
        } else {
            updatedUser = User.getById(user.id, true);
        }
        return updatedUser;
    }

    public static class UserWithPassword {
        private final String id;
        private final String password;

        private String name;
        private String description;
        private List<UserProperty> properties;

        @DataBoundConstructor
        public UserWithPassword(String id, String password) {
            this.id = id;
            this.password = password;
        }

        @DataBoundSetter
        public void setName(String name) {
            this.name = name;
        }

        @DataBoundSetter
        public void setDescription(String description) {
            this.description = description;
        }

        @DataBoundSetter
        public void setProperties(List<UserProperty> properties) {
            this.properties = properties;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public List<UserProperty> getProperties() {
            return properties;
        }
    }
}
