package org.jenkinsci.plugins.casc;

import hudson.model.Node;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class BackwardCompatibilityTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("BackwardCompatibilityTest.yml")
    public void shloud_accept_legacy_symbols_on_descriptors() throws Exception {

        final List<Node> nodes = j.jenkins.getNodes();
        System.out.println(nodes);
        assertNotNull(j.jenkins.getNode("foo"));
        assertNotNull(j.jenkins.getNode("bar"));
        assertNotNull(j.jenkins.getNode("qix"));
        // require 2.109+ assertNotNull(j.jenkins.getNode("zot"));
    }

}
