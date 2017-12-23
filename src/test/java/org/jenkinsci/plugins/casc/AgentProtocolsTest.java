package org.jenkinsci.plugins.casc;

import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * Created by odavid on 23/12/2017.
 */
public class AgentProtocolsTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void configure_agent_protocols() throws Exception {
        ConfigurationAsCode.configure(getClass().getResourceAsStream("AgentProtocolsTest.yml"));

        final Jenkins jenkins = Jenkins.getInstance();
        final Set<String> agentProtocols =
                Arrays.stream(new String[]{"JNLP4-connect", "Ping"}).collect(Collectors.toSet());
        assertEquals(agentProtocols, jenkins.getAgentProtocols());
    }
}
