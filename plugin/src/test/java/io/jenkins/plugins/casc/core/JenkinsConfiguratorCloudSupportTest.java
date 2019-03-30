package io.jenkins.plugins.casc.core;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.EphemeralNode;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.PretendSlave;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

import static io.jenkins.plugins.casc.ConfigurationAsCode.serializeYamlNode;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class JenkinsConfiguratorCloudSupportTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("JenkinsConfiguratorCloudSupportTest.yml")
    public void should_have_nodes_configured() {
        assertEquals("Base nodes not found", 2, j.jenkins.getNodes().size());
    }

    @Test
    public void should_remove_normal_nodes_configured_after_reload() throws Exception {
        final Node slave = new StaticPretendSlave();
        j.jenkins.addNode(slave);

        ConfigurationAsCode.get().configure(this.getClass().getResource("JenkinsConfiguratorCloudSupportTest.yml").toString());
        assertEquals("Base nodes not found", 2, j.jenkins.getNodes().size());
    }

    @Test
    public void should_keep_cloud_no_instantiable_nodes_configured_after_reload() throws Exception {
        final Node slave = new Cloud1PretendSlave();
        j.jenkins.addNode(slave);

        ConfigurationAsCode.get().configure(this.getClass().getResource("JenkinsConfiguratorCloudSupportTest.yml").toString());
        assertEquals("Cloud nodes not found", 3, j.jenkins.getNodes().size());
        assertNotNull("Slave 1", j.jenkins.getNode("agent1"));
        assertNotNull("Slave 1", j.jenkins.getNode("agent2"));
        assertNotNull("Slave cloud", j.jenkins.getNode("testCloud"));
    }

    @Test
    public void should_keep_cloud_ephemeral_nodes_configured_after_reload() throws Exception {
        final Node slave = new Cloud2PretendSlave();
        j.jenkins.addNode(slave);

        ConfigurationAsCode.get().configure(this.getClass().getResource("JenkinsConfiguratorCloudSupportTest.yml").toString());
        assertEquals("Cloud nodes not found", 3, j.jenkins.getNodes().size());
        assertNotNull("Slave 1", j.jenkins.getNode("agent1"));
        assertNotNull("Slave 1", j.jenkins.getNode("agent2"));
        assertNotNull("Slave cloud", j.jenkins.getNode("testCloud"));
    }

    @Test
    public void should_keep_cloud_abstractCloudSlave_nodes_configured_after_reload() throws Exception {
        final Node slave = new Cloud3PretendSlave();
        j.jenkins.addNode(slave);

        ConfigurationAsCode.get().configure(this.getClass().getResource("JenkinsConfiguratorCloudSupportTest.yml").toString());
        assertEquals("Cloud nodes not found", 3, j.jenkins.getNodes().size());
        assertNotNull("Slave 1", j.jenkins.getNode("agent1"));
        assertNotNull("Slave 1", j.jenkins.getNode("agent2"));
        assertNotNull("Slave cloud", j.jenkins.getNode("testCloud"));
    }

    @Test
    @ConfiguredWithCode("JenkinsConfiguratorCloudSupportTest.yml")
    public void should_export_only_static_nodes() throws Exception {
        j.jenkins.addNode(new Cloud1PretendSlave());
        
        final JenkinsConfigurator root = getJenkinsConfigurator();
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final CNode configNode = getNodesNode(root, context);

        final String yamlConfig = toYamlString(configNode);
        assertThat(yamlConfig, containsString("name: \"agent1\""));
        assertThat(yamlConfig, containsString("name: \"agent2\""));
        assertThat(yamlConfig, not(containsString("name: \"testCloud\"")));
    }


    private CNode getNodesNode(JenkinsConfigurator root, ConfigurationContext context) throws Exception {
        return root.describe(root.getTargetComponent(context), context).asMapping().get("nodes");
    }

    private JenkinsConfigurator getJenkinsConfigurator() {
        return j.jenkins.getExtensionList(JenkinsConfigurator.class).get(0);
    }

    private static String toYamlString(CNode rootNode) throws IOException {
        io.jenkins.plugins.casc.snakeyaml.nodes.Node yamlRoot = ConfigurationAsCode.get().toYaml(rootNode);
        StringWriter buffer = new StringWriter();
        serializeYamlNode(yamlRoot, buffer);
        return buffer.toString();
    }
    
    private static class BasePretendSlave extends PretendSlave {
        public BasePretendSlave() throws IOException, Descriptor.FormException {
            super("testCloud", "remoteFS", 3, Mode.NORMAL, "labelString", null, null);
        }
    }

    private static class StaticPretendSlave extends BasePretendSlave {
        public StaticPretendSlave() throws IOException, Descriptor.FormException {
            super();
        }
    }

    private static class Cloud1PretendSlave extends StaticPretendSlave {

        public Cloud1PretendSlave() throws IOException, Descriptor.FormException {
            super();
        }

        @Extension
        public static class DescriptorImpl extends SlaveDescriptor {
            public boolean isInstantiable() {
                return false;
            }
        }
    }

    private static class Cloud2PretendSlave extends StaticPretendSlave implements EphemeralNode {

        public Cloud2PretendSlave() throws IOException, Descriptor.FormException {
            super();
        }

        @Override
        public Node asNode() {
            return null;
        }
    }

    private static class Cloud3PretendSlave extends AbstractCloudSlave {

        public Cloud3PretendSlave() throws IOException, Descriptor.FormException {
            super("testCloud", "Node desc", "remoteFS", 3, Mode.NORMAL, "labelString", null, null, new ArrayList<>());
        }

        @Override
        public AbstractCloudComputer createComputer() {
            return null;
        }

        @Override
        protected void _terminate(TaskListener taskListener) throws IOException, InterruptedException {
            // empty
        }
    }
}
