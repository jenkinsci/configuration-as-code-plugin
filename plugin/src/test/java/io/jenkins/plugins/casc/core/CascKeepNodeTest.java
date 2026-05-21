package io.jenkins.plugins.casc.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import hudson.slaves.DumbSlave;
import hudson.slaves.JNLPLauncher;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Tests that {@link CascKeepNode} causes the node to survive a JCasc reload.
 */
public class CascKeepNodeTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    /**
     * A DumbSlave with a CascKeepNode property should survive a JCasc reload that contains
     * no nodes: section for it.
     */
    @Test
    public void nodeWithCascKeepNodePropertySurvivesReload() throws Exception {
        DumbSlave node = new DumbSlave("keep-me", "/tmp/workspace", new JNLPLauncher(true));
        node.setNodeProperties(Collections.singletonList(new CascKeepNode()));
        j.jenkins.addNode(node);

        assertNotNull("Node should exist before reload", j.jenkins.getNode("keep-me"));

        // Reload from an empty config — no nodes: block, so reconcile would normally
        // delete every node not listed in the YAML.
        ConfigurationAsCode.get()
                .configure(getClass().getResource("empty-casc.yaml").toExternalForm());

        assertNotNull("Node should survive reload because it has CascKeepNode", j.jenkins.getNode("keep-me"));
    }

    /**
     * A plain DumbSlave WITHOUT CascKeepNode should be removed during reload when it is
     * not listed in the YAML nodes: block.
     */
    @Test
    public void nodeWithoutCascKeepNodeIsRemovedOnReload() throws Exception {
        DumbSlave node = new DumbSlave("remove-me", "/tmp/workspace", new JNLPLauncher(true));
        j.jenkins.addNode(node);

        assertNotNull("Node should exist before reload", j.jenkins.getNode("remove-me"));

        ConfigurationAsCode.get()
                .configure(getClass().getResource("empty-casc.yaml").toExternalForm());

        assertEquals(
                "Plain node without CascKeepNode should be deleted on reload", null, j.jenkins.getNode("remove-me"));
    }
}
