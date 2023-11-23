package io.jenkins.plugins.casc.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import hudson.model.ComputerSet;
import hudson.node_monitors.ArchitectureMonitor;
import hudson.node_monitors.DiskSpaceMonitor;
import hudson.node_monitors.ResponseTimeMonitor;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;

public class NodeMonitorsConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("NodeMonitors.yml")
    public void should_configure_node_monitors() {
        DiskSpaceMonitor dsm = (DiskSpaceMonitor) ComputerSet.getMonitors().get(DiskSpaceMonitor.DESCRIPTOR);
        assertThat(dsm.freeSpaceThreshold, is("3GB"));
        ArchitectureMonitor.DescriptorImpl amd =
                (ArchitectureMonitor.DescriptorImpl) Jenkins.get().getDescriptorOrDie(ArchitectureMonitor.class);
        ArchitectureMonitor am = (ArchitectureMonitor) ComputerSet.getMonitors().get(amd);
        assertThat(am.isIgnored(), is(false));
    }

    @Test
    @ConfiguredWithCode("NodeMonitors.yml")
    public void not_configured_monitors_are_ignored() {
        ResponseTimeMonitor rtm =
                (ResponseTimeMonitor) ComputerSet.getMonitors().get(ResponseTimeMonitor.DESCRIPTOR);
        assertThat(rtm.isIgnored(), is(true));
    }
}
