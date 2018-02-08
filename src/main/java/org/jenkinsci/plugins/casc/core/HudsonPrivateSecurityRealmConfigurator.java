package org.jenkinsci.plugins.casc.core;

import hudson.Extension;
import hudson.security.HudsonPrivateSecurityRealm;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.DataBoundConfigurator;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;
import java.util.Set;

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
        describe.add(new Attribute("users", UserWithPassword.class).multiple(true).setter((target, attribute, value) -> {
            HudsonPrivateSecurityRealm realm = (HudsonPrivateSecurityRealm) target;
            final List<UserWithPassword> users = (List<UserWithPassword>) value;
            for (UserWithPassword user : users) {
                realm.createAccount(user.id, user.password);
            }
        }));
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
