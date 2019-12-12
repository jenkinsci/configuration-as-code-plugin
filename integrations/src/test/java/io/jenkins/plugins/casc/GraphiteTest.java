package io.jenkins.plugins.casc;

import hudson.ExtensionList;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.metrics.impl.graphite.GraphiteServer;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author v1v (Victor Martinez)
 */
public class GraphiteTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("graphite/README.md")
    public void configure_graphite() {
        final GraphiteServer.DescriptorImpl descriptor = ExtensionList.lookupSingleton(GraphiteServer.DescriptorImpl.class);
        assertNotNull(descriptor);
        assertEquals(1, descriptor.getServers().size());
        assertEquals("1.2.3.4", descriptor.getServers().get(0).getHostname());
        assertEquals(2003, descriptor.getServers().get(0).getPort());
        assertEquals("jenkins.master.", descriptor.getServers().get(0).getPrefix());
    }
}
