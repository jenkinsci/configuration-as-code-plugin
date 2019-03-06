package io.jenkins.plugins.casc;

import hudson.model.Node;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class BackwardCompatibilityTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("BackwardCompatibilityTest.yml")
    public void should_accept_legacy_symbols_on_descriptors() throws Exception {

        final List<Node> nodes = j.jenkins.getNodes();
        System.out.println(nodes);
        assertNotNull(j.jenkins.getNode("foo"));
        assertNotNull(j.jenkins.getNode("bar"));
        assertNotNull(j.jenkins.getNode("qix"));
        // see # see https://github.com/jenkinsci/jenkins/pull/3475
        // assertNotNull(j.jenkins.getNode("zot"));

        final List<ObsoleteConfigurationMonitor.Error> errors = ObsoleteConfigurationMonitor.get().getErrors();
        assertEquals("'DumbSlave' is obsolete, please use 'dumb'", errors.get(0).message);
    }

}
