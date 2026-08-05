package io.jenkins.plugins.casc.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import hudson.model.FreeStyleProject;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Sequence;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ItemExporterTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testExportFreestyleProject() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("test-job");
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        ItemExporter exporter = new ItemExporter();
        CNode rootNode = exporter.export(project, context);

        assertNotNull("The exported CNode should not be null", rootNode);
        assertTrue("The root node should be a Mapping", rootNode instanceof Mapping);

        Mapping rootMapping = (Mapping) rootNode;
        assertTrue("Root should contain 'items' key", rootMapping.containsKey("items"));

        Sequence itemsSeq = (Sequence) rootMapping.get("items");
        assertEquals("Should have exactly one item in the sequence", 1, itemsSeq.size());

        Mapping itemMapping = (Mapping) itemsSeq.get(0);
        assertTrue(
                "The item should be keyed by its configurator name 'freestyle'", itemMapping.containsKey("freestyle"));

        Mapping freestyleProps = (Mapping) itemMapping.get("freestyle");
        assertNotNull("Freestyle properties should not be null", freestyleProps.get("name"));
        assertEquals("test-job", freestyleProps.getScalarValue("name"));
    }
}
