package org.jenkinsci.plugins.casc;

import hudson.security.AuthorizationStrategy;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.security.HudsonPrivateSecurityRealm;
import jenkins.model.Jenkins;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class JenkinsConfiguratorTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void jenkins_primitive_attributes() throws Exception {
        ConfigurationAsCode.configure(getClass().getResourceAsStream("Primitives.yml"));

        final Jenkins jenkins = Jenkins.getInstance();
        assertEquals(6666, jenkins.getSlaveAgentPort());
        assertEquals(false, jenkins.isUsageStatisticsCollected());
    }

    @Test
    public void jenkins_abstract_describable_attributes() throws Exception {
        ConfigurationAsCode.configure(getClass().getResourceAsStream("HeteroDescribable.yml"));

        final Jenkins jenkins = Jenkins.getInstance();
        assertTrue(jenkins.getSecurityRealm() instanceof HudsonPrivateSecurityRealm);
        assertTrue(jenkins.getAuthorizationStrategy() instanceof FullControlOnceLoggedInAuthorizationStrategy);
        assertFalse(((FullControlOnceLoggedInAuthorizationStrategy) jenkins.getAuthorizationStrategy()).isAllowAnonymousRead());
    }

    @Test
    @Issue("Issue #60")
    public void shouldHaveAuthStrategyConfigurator() throws Exception {
        Configurator c = Configurator.lookup(Jenkins.class);
        Attribute attr = c.getAttribute("authorizationStrategy");
        assertNotNull(attr);
        // Apparently Java always thinks that labmdas are equal
        //assertTrue("The operation should not be NOOP", JenkinsConfigurator.NOOP != attr.getSetter());
        attr.getSetter().setValue(j.jenkins, attr, new AuthorizationStrategy.Unsecured());

        assertThat("Authorization strategy has not been set",
                j.jenkins.getAuthorizationStrategy(),
                CoreMatchers.instanceOf(AuthorizationStrategy.Unsecured.class));
    }

}
