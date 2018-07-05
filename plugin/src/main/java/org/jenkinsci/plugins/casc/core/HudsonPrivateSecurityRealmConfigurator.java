package org.jenkinsci.plugins.casc.core;

import hudson.Extension;
import hudson.security.HudsonPrivateSecurityRealm;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.DataBoundConfigurator;
import org.jenkinsci.plugins.casc.MultivaluedAttribute;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class HudsonPrivateSecurityRealmConfigurator extends DataBoundConfigurator {

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
                    target.createAccount(user.id, user.password);
                }
            }
        ));
        return describe;
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
