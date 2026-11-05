package io.jenkins.plugins.casc.core;

import static hudson.util.StreamTaskListener.fromStdout;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import hudson.model.Computer;
import hudson.model.Node;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import java.util.Objects;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class DisconnectedOnStartupTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void should_mark_node_offline_on_startup() {
        String yaml = Objects.requireNonNull(getClass().getResource("disconnectedOnStartup.yml"))
                .toExternalForm();

        ConfigurationAsCode.get().configure(yaml);

        Node node = Jenkins.get().getNode("test-node-enabled");
        assertNotNull(node);

        Computer computer = node.toComputer();
        assertNotNull(computer);

        assertThat(computer.getOfflineCause(), instanceOf(JCasCOfflineCause.class));
        assertThat(computer.getOfflineCause().toString(), is("JCasC Startup State: Maintenance Mode"));
    }

    @Test
    public void should_not_mark_node_offline_when_disabled() {
        String yaml = Objects.requireNonNull(getClass().getResource("disconnectedOnStartupDisabled.yml"))
                .toExternalForm();

        ConfigurationAsCode.get().configure(yaml);

        Node node = Jenkins.get().getNode("test-node-disabled");
        assertNotNull(node);

        Computer computer = node.toComputer();
        assertNotNull(computer);

        assertThat(computer.getOfflineCause() instanceof JCasCOfflineCause, is(false));
    }

    @Test
    public void should_keep_node_online_when_property_not_defined() {
        String yaml = Objects.requireNonNull(
                        getClass().getResource("noDisconnectedProperty.yml"), "YAML resource not found")
                .toExternalForm();

        ConfigurationAsCode.get().configure(yaml);

        Node node = Jenkins.get().getNode("test-node-default");
        assertNotNull(node);

        Computer computer = node.toComputer();
        assertNotNull(computer);

        assertThat(computer.getOfflineCause() instanceof JCasCOfflineCause, is(false));
    }

    @Test
    public void should_reconnect_node_when_property_disabled_after_being_enabled() throws Exception {
        String yamlEnabled = Objects.requireNonNull(getClass().getResource("disconnectedOnStartup.yml"))
                .toExternalForm();

        ConfigurationAsCode.get().configure(yamlEnabled);

        Node node = Jenkins.get().getNode("test-node-enabled");
        assertNotNull(node);

        Computer computer = node.toComputer();
        assertNotNull(computer);

        assertThat(computer.getOfflineCause(), instanceOf(JCasCOfflineCause.class));

        j.waitUntilNoActivity();

        String yamlToggled = Objects.requireNonNull(getClass().getResource("disconnectedOnStartupToggle.yml"))
                .toExternalForm();

        ConfigurationAsCode.get().configure(yamlToggled);

        Computer updatedComputer = Objects.requireNonNull(Jenkins.get().getNode("test-node-enabled"))
                .toComputer();
        assertNotNull(updatedComputer);

        j.waitUntilNoActivity();

        assertThat(updatedComputer.getOfflineCause() instanceof JCasCOfflineCause, is(false));

        assertThat(updatedComputer.isConnecting(), is(false));
    }

    @Test
    public void should_prevent_launch_when_disconnected_on_startup_enabled() throws Exception {
        String yaml = Objects.requireNonNull(getClass().getResource("disconnectedOnStartup.yml"))
                .toExternalForm();

        ConfigurationAsCode.get().configure(yaml);

        Node node = Jenkins.get().getNode("test-node-enabled");
        assertNotNull(node);

        Computer computer = node.toComputer();
        assertNotNull(computer);

        computer.connect(false);

        j.waitUntilNoActivity();

        assertThat(computer.getOfflineCause(), instanceOf(JCasCOfflineCause.class));
    }

    @Test
    public void should_safely_ignore_computer_with_null_node() throws Exception {
        Node tempNode = j.createSlave("temp-node", "label", null);
        Computer computer = tempNode.toComputer();
        assertNotNull(computer);

        Jenkins.get().removeNode(tempNode);
        assertNull("Node should be null after removal", computer.getNode());

        DisconnectedOnStartupListener listener = new DisconnectedOnStartupListener();

        listener.preLaunch(computer, fromStdout());

        assertTrue("Listener safely ignored the null node", true);
    }
}
