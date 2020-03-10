package io.jenkins.plugins.casc.core;

import hudson.model.User;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.security.HudsonPrivateSecurityRealm;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.model.Jenkins;
import jenkins.plugins.slack.user.SlackUserProperty;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class HudsonPrivateSecurityRealmConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("embedded-userdatabase/README.md#0")
    public void configure_local_security_and_admin_user() throws Exception {
        final Jenkins jenkins = Jenkins.get();
        final HudsonPrivateSecurityRealm securityRealm = (HudsonPrivateSecurityRealm) jenkins.getSecurityRealm();
        assertFalse(securityRealm.allowsSignup());
        final User admin = User.getById("admin", false);
        assertNotNull(admin);
        final HudsonPrivateSecurityRealm.Details details = admin.getProperty(HudsonPrivateSecurityRealm.Details.class);
        assertTrue(details.isPasswordCorrect("somethingsecret"));

        final FullControlOnceLoggedInAuthorizationStrategy authorizationStrategy = (FullControlOnceLoggedInAuthorizationStrategy) jenkins.getAuthorizationStrategy();
        assertTrue(authorizationStrategy.isAllowAnonymousRead());
    }

    @Test
    @ConfiguredWithReadme("embedded-userdatabase/README.md#1")
    public void config_local_security_and_hashed_admin_user() {
        final User admin = User.getById("hashedadmin", false);
        assertNotNull(admin);
        final HudsonPrivateSecurityRealm.Details details = admin.getProperty(HudsonPrivateSecurityRealm.Details.class);
        assertTrue(details.isPasswordCorrect("password"));
    }

    @Test
    @ConfiguredWithReadme("embedded-userdatabase/README.md#1")
    public void configure_all_attributes() {
        final User admin = User.getById("admin", false);
        assertNotNull(admin);

        assertThat(admin.getFullName(), is("Admin"));
        assertThat(admin.getDescription(), is("Superwoman"));

        SlackUserProperty slackUserProperty = admin
            .getProperty(SlackUserProperty.class);
        assertThat(slackUserProperty.getUserId(), is("ABCDEFGH"));

//        pending https://github.com/jenkinsci/mailer-plugin/pull/80
//        UserProperty mailerProperty = admin.getProperty(UserProperty.class);
//        assertThat(mailerProperty.getEmailAddress(), is("admin3@example.com"));

// Pending https://github.com/jenkinsci/ssh-cli-auth-module/pull/16
//        UserPropertyImpl authorizedKeysProperty = admin.getProperty(UserPropertyImpl.class);
//        assertThat(authorizedKeysProperty.authorizedKeys, is("ssh-rsa some-key\n"));
    }

}
