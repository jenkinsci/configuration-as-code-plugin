package io.jenkins.plugins.casc.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import hudson.model.ComputerSet;
import hudson.node_monitors.ArchitectureMonitor;
import hudson.node_monitors.ClockMonitor;
import hudson.node_monitors.DiskSpaceMonitor;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;

public class NodeMonitorsConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("node-monitors/README.md")
    public void should_configure_node_monitors() {
        DiskSpaceMonitor dsm = (DiskSpaceMonitor) ComputerSet.getMonitors().get(DiskSpaceMonitor.DESCRIPTOR);
        assertThat(dsm.freeSpaceThreshold, is("3GB"));
        ArchitectureMonitor.DescriptorImpl amd =
                (ArchitectureMonitor.DescriptorImpl) Jenkins.get().getDescriptorOrDie(ArchitectureMonitor.class);
        ArchitectureMonitor am = (ArchitectureMonitor) ComputerSet.getMonitors().get(amd);
        assertThat(am.isIgnored(), is(false));
    }

    @Test
    @ConfiguredWithReadme("node-monitors/README.md")
    public void not_configured_monitors_are_ignored() {
        ClockMonitor.DescriptorImpl cmd =
                (ClockMonitor.DescriptorImpl) Jenkins.get().getDescriptorOrDie(ClockMonitor.class);
        ClockMonitor cm = (ClockMonitor) ComputerSet.getMonitors().get(cmd);
        assertThat(cm.isIgnored(), is(true));
    }
}
