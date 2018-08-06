package io.jenkins.plugins.casc.core;

import hudson.model.User;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.security.HudsonPrivateSecurityRealm;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import jenkins.model.Jenkins;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class HudsonPrivateSecurityRealmConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

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

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final Configurator c = context.lookupOrFail(HudsonPrivateSecurityRealm.class);
        final CNode node = c.describe(securityRealm, context);
        final Mapping user = node.asMapping().get("users").asSequence().get(0).asMapping();
        assertEquals("admin", user.getScalarValue("id"));
    }
}
