package io.jenkins.plugins.casc.core;

import hudson.EnvVars;
import hudson.model.TaskListener;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.io.ByteArrayOutputStream;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        final Jenkins jenkins = Jenkins.get();
        assertEquals(6666, jenkins.getSlaveAgentPort());
    }

    @Test
    @ConfiguredWithCode("HeteroDescribable.yml")
    public void jenkins_abstract_describable_attributes() throws Exception {
        final Jenkins jenkins = Jenkins.get();
        assertTrue(jenkins.getSecurityRealm() instanceof HudsonPrivateSecurityRealm);
        assertTrue(jenkins.getAuthorizationStrategy() instanceof FullControlOnceLoggedInAuthorizationStrategy);
        assertFalse(((FullControlOnceLoggedInAuthorizationStrategy) jenkins.getAuthorizationStrategy()).isAllowAnonymousRead());
    }

    @Test
    @Issue("Issue #173")
    @ConfiguredWithCode("SetEnvironmentVariable.yml")
    public void shouldSetEnvironmentVariable() throws Exception {
        final DescribableList<NodeProperty<?>, NodePropertyDescriptor> properties = Jenkins.get().getNodeProperties();
        EnvVars env = new EnvVars();
        for (NodeProperty<?> property : properties) {
            property.buildEnvVars(env, TaskListener.NULL);
        }
        assertEquals("BAR", env.get("FOO"));
    }

    @Test
    @ConfiguredWithCode("ConfigureNode.yml")
    public void shouldExportLabelAtoms() throws Exception {
        Jenkins.get().getLabelAtom("label1").getProperties().add(new TestProperty(2));

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConfigurationAsCode.get().export(out);
        final String s = out.toString();

        assertThat(s, is(not(emptyOrNullString())));
        assertThat(s, not(containsString("FAILED TO EXPORT")));
    }

}
