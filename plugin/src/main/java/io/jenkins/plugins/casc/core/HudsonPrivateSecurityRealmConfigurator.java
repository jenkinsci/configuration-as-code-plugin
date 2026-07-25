package io.jenkins.plugins.casc.core;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.util.Secret;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.impl.attributes.MultivaluedAttribute;
import io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jenkins.security.ApiTokenProperty;
import org.apache.commons.lang3.StringUtils;
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
    // matches HudsonPrivateSecurityRealm.JBCRYPT_HEADER
    private static final String HASHED_PASSWORD_PREFIX = "#jbcrypt:";
    private static final String MASKED_TOKEN_VALUE = "****";

    public HudsonPrivateSecurityRealmConfigurator() {
        super(HudsonPrivateSecurityRealm.class);
    }

    @NonNull
    @Override
    public Set<Attribute<HudsonPrivateSecurityRealm, ?>> describe() {
        final Set<Attribute<HudsonPrivateSecurityRealm, ?>> describe = super.describe();
        describe.add(
                new MultivaluedAttribute<HudsonPrivateSecurityRealm, UserWithPassword>("users", UserWithPassword.class)
                        .getter(HudsonPrivateSecurityRealmConfigurator::getter)
                        .setter(HudsonPrivateSecurityRealmConfigurator::setter));
        return describe;
    }

    @CheckForNull
    @Override
    public CNode describe(HudsonPrivateSecurityRealm instance, ConfigurationContext context) throws Exception {
        // allow disabling exporting users if an instance has too many
        if (System.getProperty(
                        "io.jenkins.plugins.casc.core.HudsonPrivateSecurityRealmConfigurator.exportUsers", "true")
                .equals("true")) {
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
                    List<UserProperty> properties = u.getAllProperties().stream()
                            .filter(
                                    userProperty -> !userProperty
                                            .getClass()
                                            .getName()
                                            .equals(
                                                    "com.cloudbees.plugins.credentials.UserCredentialsProvider$UserCredentialsProperty"))
                            .collect(Collectors.toList());
                    user.setProperties(properties);

                    ApiTokenProperty tokenProperty = u.getProperty(ApiTokenProperty.class);
                    if (tokenProperty != null) {
                        try {
                            Method getTokenListMethod = ApiTokenProperty.class.getMethod("getTokenList");
                            Collection<?> tokenList = (Collection<?>) getTokenListMethod.invoke(tokenProperty);

                            if (tokenList != null && !tokenList.isEmpty()) {
                                List<ApiToken> exportedTokens = tokenList.stream()
                                        .map(t -> {
                                            try {
                                                String name;
                                                try {
                                                    name = (String) t.getClass()
                                                            .getField("name")
                                                            .get(t);
                                                } catch (ReflectiveOperationException e) {
                                                    name = (String) t.getClass()
                                                            .getMethod("getName")
                                                            .invoke(t);
                                                }
                                                return new ApiToken(name, Secret.fromString(MASKED_TOKEN_VALUE));
                                            } catch (ReflectiveOperationException e) {
                                                return null;
                                            }
                                        })
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList());
                                user.setApiTokens(exportedTokens);
                            }
                        } catch (ReflectiveOperationException ignored) {
                        }
                    }
                    return user;
                })
                .collect(Collectors.toList());
    }

    private static void setter(HudsonPrivateSecurityRealm target, Collection<UserWithPassword> value)
            throws IOException {
        for (UserWithPassword user : value) {
            User updatedUser = createAccount(target, user);
            updatedUser.setFullName(user.name);
            updatedUser.setDescription(user.description);
            if (user.getProperties() != null) {
                for (UserProperty property : user.getProperties()) {
                    updatedUser.addProperty(property);
                }
            }

            if (user.getApiTokens() != null && !user.getApiTokens().isEmpty()) {
                ApiTokenProperty tokenProperty = updatedUser.getProperty(ApiTokenProperty.class);
                if (tokenProperty == null) {
                    tokenProperty = new ApiTokenProperty();
                    updatedUser.addProperty(tokenProperty);
                }

                try {
                    Method getTokenListMethod = ApiTokenProperty.class.getMethod("getTokenList");
                    Collection<?> tokenList = (Collection<?>) getTokenListMethod.invoke(tokenProperty);

                    Method getTokenStoreMethod = ApiTokenProperty.class.getMethod("getTokenStore");
                    Object tokenStore = getTokenStoreMethod.invoke(tokenProperty);

                    Method revokeTokenMethod = null;
                    Method addFixedNewTokenMethod = null;

                    for (Method m : tokenStore.getClass().getMethods()) {
                        if ("revokeToken".equals(m.getName()) && m.getParameterCount() == 1) {
                            revokeTokenMethod = m;
                        } else if ("addFixedNewToken".equals(m.getName()) && m.getParameterCount() == 2) {
                            addFixedNewTokenMethod = m;
                        }
                    }

                    if (addFixedNewTokenMethod != null) {
                        for (ApiToken apiToken : user.getApiTokens()) {
                            if (MASKED_TOKEN_VALUE.equals(Secret.toString(apiToken.token()))) {
                                continue;
                            }

                            if (tokenList != null) {
                                for (Object t : tokenList) {
                                    String tName;
                                    String tUuid;
                                    try {
                                        tName = (String)
                                                t.getClass().getField("name").get(t);
                                        tUuid = (String)
                                                t.getClass().getField("uuid").get(t);
                                    } catch (ReflectiveOperationException ex) {
                                        tName = (String) t.getClass()
                                                .getMethod("getName")
                                                .invoke(t);
                                        tUuid = (String) t.getClass()
                                                .getMethod("getUuid")
                                                .invoke(t);
                                    }

                                    if (Objects.equals(tName, apiToken.name()) && revokeTokenMethod != null) {
                                        revokeTokenMethod.invoke(tokenStore, tUuid);
                                    }
                                }
                            }

                            addFixedNewTokenMethod.invoke(
                                    tokenStore, apiToken.name(), Secret.toString(apiToken.token()));
                        }
                    } else {
                        throw new IOException("ApiTokenStore does not expose addFixedNewToken(String, String)");
                    }
                } catch (ReflectiveOperationException e) {
                    throw new IOException("Failed to configure API tokens via reflection", e);
                }
            }
            updatedUser.save();
        }
    }

    private static User createAccount(HudsonPrivateSecurityRealm target, UserWithPassword user) throws IOException {
        User updatedUser;
        if (StringUtils.isNotBlank(user.password)) {
            if (user.password.startsWith(HASHED_PASSWORD_PREFIX)) {
                updatedUser = target.createAccountWithHashedPassword(user.id, user.password);
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
        private List<ApiToken> apiTokens;

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

        @DataBoundSetter
        public void setApiTokens(List<ApiToken> apiTokens) {
            this.apiTokens = apiTokens;
        }

        public List<ApiToken> getApiTokens() {
            return apiTokens;
        }
    }

    public record ApiToken(String name, Secret token) {

        @DataBoundConstructor
        public ApiToken {}
    }
}
