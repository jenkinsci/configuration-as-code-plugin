package org.jenkinsci.plugins.casc;

import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.security.HudsonPrivateSecurityRealm;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
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



}
