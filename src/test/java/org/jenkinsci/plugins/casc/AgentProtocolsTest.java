package org.jenkinsci.plugins.casc;

import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.misc.CodeConfiguratorRunner;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * Created by odavid on 23/12/2017.
 */
public class AgentProtocolsTest {

    public JenkinsRule j = new JenkinsRule();
    public CodeConfiguratorRunner config = new CodeConfiguratorRunner();

    @Rule
    public RuleChain chain = RuleChain.outerRule(j).around(config);

    @Test
    @ConfiguredWithCode(value = "AgentProtocolsTest.yml")
    public void configure_agent_protocols() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();
        final Set<String> agentProtocols =
                Arrays.stream(new String[]{"JNLP4-connect", "Ping"}).collect(Collectors.toSet());
        assertEquals(agentProtocols, jenkins.getAgentProtocols());
    }
}
