package org.jenkinsci.plugins.casc.core;

import hudson.model.User;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.security.HudsonPrivateSecurityRealm;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.misc.CodeConfiguratorRunner;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class HudsonPrivateSecurityRealmConfiguratorTest {

    public JenkinsRule j = new JenkinsRule();
    public CodeConfiguratorRunner config = new CodeConfiguratorRunner();

    @Rule
    public RuleChain chain = RuleChain.outerRule(j).around(config);

    @Test
    @ConfiguredWithCode("HudsonPrivateSecurityRealmConfiguratorTest.yml")
    public void configure_local_security_and_admin_user() throws Exception {
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
