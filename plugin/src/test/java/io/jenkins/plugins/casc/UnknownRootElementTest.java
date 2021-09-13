package io.jenkins.plugins.casc;

import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;

public class UnknownRootElementTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void oneUnknown() {
        assertThrows("No configurator for the following root elements alice",
            ConfiguratorException.class,
            () -> ConfigurationAsCode.get()
                .configure(getClass().getResource("unknown1.yml").toExternalForm()));
    }

    @Test
    public void twoUnknown() {
        assertThrows("No configurator for the following root elements bob, alice",
            ConfiguratorException.class,
            () -> ConfigurationAsCode.get()
                .configure(getClass().getResource("unknown2.yml").toExternalForm()));
    }

    @Test
    public void ignoreKnownAlias() throws Exception {
        ConfigurationAsCode.get().configure(getClass().getResource("known.yml").toExternalForm());
        assertThat(j.jenkins.getSystemMessage(), is("Configured by Configuration as Code plugin"));
        Set<String> agentProtocols = j.jenkins.getAgentProtocols();
        assertThat(agentProtocols, hasSize(2));
    }
}
