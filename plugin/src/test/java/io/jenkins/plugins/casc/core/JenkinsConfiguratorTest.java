package io.jenkins.plugins.casc.core;

import hudson.EnvVars;
import hudson.model.TaskListener;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class JenkinsConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("Primitives.yml")
    public void jenkins_primitive_attributes() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();
        assertEquals(6666, jenkins.getSlaveAgentPort());
        assertEquals(false, jenkins.isUsageStatisticsCollected());
    }

    @Test
    @ConfiguredWithCode("HeteroDescribable.yml")
    public void jenkins_abstract_describable_attributes() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();
        assertTrue(jenkins.getSecurityRealm() instanceof HudsonPrivateSecurityRealm);
        assertTrue(jenkins.getAuthorizationStrategy() instanceof FullControlOnceLoggedInAuthorizationStrategy);
        assertFalse(((FullControlOnceLoggedInAuthorizationStrategy) jenkins.getAuthorizationStrategy()).isAllowAnonymousRead());
    }

    @Test
    @Issue("Issue #173")
    @ConfiguredWithCode("SetEnvironmentVariable.yml")
    public void shouldSetEnvironmentVariable() throws Exception {
        final DescribableList<NodeProperty<?>, NodePropertyDescriptor> properties = Jenkins.getInstance().getNodeProperties();
        EnvVars env = new EnvVars();
        for (NodeProperty<?> property : properties) {
            property.buildEnvVars(env, TaskListener.NULL);
        }
        assertEquals("BAR", env.get("FOO"));
    }

}
