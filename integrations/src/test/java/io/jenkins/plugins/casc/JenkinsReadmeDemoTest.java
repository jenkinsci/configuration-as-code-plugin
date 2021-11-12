package io.jenkins.plugins.casc;

import hudson.model.Node.Mode;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;


/**
 * @author v1v (Victor Martinez)
 */
public class JenkinsReadmeDemoTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("jenkins/README.md#0")
    public void configure_demo_first_code_block() {
        final Jenkins jenkins = Jenkins.get();
        assertEquals("Jenkins configured automatically by Jenkins Configuration as Code plugin\n\n", jenkins.getSystemMessage());
        assertEquals(5, jenkins.getNumExecutors());
        assertEquals(2, jenkins.getScmCheckoutRetryCount());
        assertEquals(Mode.NORMAL, jenkins.getMode());
    }

    @Test
    @ConfiguredWithReadme("jenkins/README.md#1")
    public void configure_demo_second_code_block() {
        final Jenkins jenkins = Jenkins.get();
        assertThat(jenkins.getSystemMessage(), containsString("Welcome to our build server."));
        assertEquals(1, jenkins.getNumExecutors());
    }
}
