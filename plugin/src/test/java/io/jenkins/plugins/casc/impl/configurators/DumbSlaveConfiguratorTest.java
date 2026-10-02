package io.jenkins.plugins.casc.impl.configurators;

import static java.nio.file.Files.writeString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import hudson.model.Node;
import hudson.slaves.DumbSlave;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class DumbSlaveConfiguratorTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testBackwardCompatibility_OnlyLabelString() throws Exception {
        String yaml = """
                jenkins:
                  nodes:
                    - permanent:\s
                        name: "node-1"
                        remoteFS: "/tmp"
                        launcher: "jnlp"
                        labelString: "common-1 common-2"
                """;

        configureWithYaml(yaml);

        DumbSlave slave = getDumbSlave("node-1");
        assertEquals("common-1 common-2", slave.getLabelString());
    }

    @Test
    public void testLabelsArray_FlatAndNested() throws Exception {
        String yaml = """
                jenkins:
                  nodes:
                    - permanent:\s
                        name: "node-2"
                        remoteFS: "/tmp"
                        launcher: "jnlp"
                        labels:
                          - "label-1"
                          - "label-2"
                          - ["nested-1", "nested-2"]
                """;

        configureWithYaml(yaml);

        DumbSlave slave = getDumbSlave("node-2");
        assertEquals("label-1 label-2 nested-1 nested-2", slave.getLabelString());
    }

    @Test
    public void testMergeLabelStringAndLabels() throws Exception {
        String yaml = """
                x-node-defaults: &node-defaults
                  labelString: "common-label-1 common-label-2"
                  remoteFS: "/tmp"
                  launcher: "jnlp"

                jenkins:
                  nodes:
                    - permanent:\s
                        name: "node-3"
                        <<: *node-defaults
                        labels:
                          - "node-specific-1"
                          - ["node-specific-2", "node-specific-3"]
                """;

        configureWithYaml(yaml);

        DumbSlave slave = getDumbSlave("node-3");

        assertEquals(
                "common-label-1 common-label-2 node-specific-1 node-specific-2 node-specific-3",
                slave.getLabelString());
    }

    @Test
    public void testEmptyAndNullLabelsHandledGracefully() throws Exception {
        String yaml = """
                jenkins:
                  nodes:
                    - permanent:\s
                        name: "node-4"
                        remoteFS: "/tmp"
                        launcher: "jnlp"
                        labels:
                          - ""
                          - []
                          - "valid-label"
                """;

        configureWithYaml(yaml);

        DumbSlave slave = getDumbSlave("node-4");
        assertEquals("valid-label", slave.getLabelString());
    }

    @Test
    public void testLabelsArrayWithoutLabelString() throws Exception {
        String yaml = """
            jenkins:
              nodes:
                - permanent:
                    name: "node-5"
                    remoteFS: "/tmp"
                    launcher: "jnlp"
                    labels:
                      - "label-1"
                      - "label-2"
            """;

        configureWithYaml(yaml);

        DumbSlave slave = getDumbSlave("node-5");

        assertEquals("label-1 label-2", slave.getLabelString());
    }

    @Test
    public void testExportDumbSlaveConfiguredWithLabels() throws Exception {
        String yaml = """
            jenkins:
              nodes:
                - permanent:
                    name: "node-export"
                    remoteFS: "/tmp"
                    launcher: "jnlp"
                    labels:
                      - "label-1"
                      - "label-2"
                      - ["nested-1", "nested-2"]
            """;

        configureWithYaml(yaml);

        DumbSlave slave = getDumbSlave("node-export");
        Configurator<DumbSlave> configurator = ConfiguratorRegistry.get().lookupOrFail(DumbSlave.class);
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        CNode cnode = configurator.describe(slave, context);

        assertNotNull(cnode);
        assertTrue(cnode instanceof Mapping);
        Mapping mapping = (Mapping) cnode;

        assertEquals(
                "label-1 label-2 nested-1 nested-2",
                mapping.get("labelString").asScalar().getValue());
    }

    @Test
    public void testDumbSlaveConfiguratorIsRegistered() {
        Configurator<?> configurator = ConfiguratorRegistry.get().lookupOrFail(DumbSlave.class);

        assertTrue(configurator instanceof DumbSlaveConfigurator);
    }

    private void configureWithYaml(String yaml) throws Exception {
        File tempYamlFile = File.createTempFile("casc-test", ".yaml");
        tempYamlFile.deleteOnExit();

        writeString(tempYamlFile.toPath(), yaml);

        ConfigurationAsCode.get().configure(tempYamlFile.getAbsolutePath());
    }

    private DumbSlave getDumbSlave(String name) {
        Node node = j.jenkins.getNode(name);
        assertNotNull("Node '" + name + "' should be created", node);
        assertTrue("Node should be an instance of DumbSlave", node instanceof DumbSlave);
        return (DumbSlave) node;
    }
}
