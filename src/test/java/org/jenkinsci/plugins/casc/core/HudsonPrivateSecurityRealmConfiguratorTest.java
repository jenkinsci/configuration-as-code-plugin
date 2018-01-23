package org.jenkinsci.plugins.casc.core;

import hudson.model.User;
import hudson.security.AuthorizationStrategy;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.security.HudsonPrivateSecurityRealm;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.ConfigurationAsCode;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class HudsonPrivateSecurityRealmConfiguratorTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void configure_local_security_and_admin_user() throws Exception {
        ConfigurationAsCode.configure(getClass().getResourceAsStream("HudsonPrivateSecurityRealmConfiguratorTest.yml"));

        final Jenkins jenkins = Jenkins.getInstance();
        final HudsonPrivateSecurityRealm securityRealm = (HudsonPrivateSecurityRealm) jenkins.getSecurityRealm();
        assertFalse(securityRealm.allowsSignup());
        final User admin = User.getById("admin", false);
        assertNotNull(admin);
        final HudsonPrivateSecurityRealm.Details details = admin.getProperty(HudsonPrivateSecurityRealm.Details.class);
        assertTrue(details.isPasswordCorrect("1234"));

        final FullControlOnceLoggedInAuthorizationStrategy authorizationStrategy = (FullControlOnceLoggedInAuthorizationStrategy) jenkins.getAuthorizationStrategy();
        assertTrue(authorizationStrategy.isAllowAnonymousRead());
    }
}
