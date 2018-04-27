package org.jenkinsci.plugins.casc;

import hudson.EnvVars;
import hudson.model.TaskListener;
import hudson.security.AuthorizationStrategy;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import org.hamcrest.CoreMatchers;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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
