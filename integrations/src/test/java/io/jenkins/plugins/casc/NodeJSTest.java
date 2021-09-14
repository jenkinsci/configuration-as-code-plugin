package io.jenkins.plugins.casc;

import hudson.ExtensionList;
import hudson.tools.InstallSourceProperty;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import jenkins.plugins.nodejs.tools.NodeJSInstaller;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NodeJSTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("nodejs/README.md")
    public void configure_nodejs() {
        final NodeJSInstallation.DescriptorImpl descriptor = ExtensionList.lookupSingleton(NodeJSInstallation.DescriptorImpl.class);
        assertEquals(1, descriptor.getInstallations().length);

        final NodeJSInstallation nodejs = descriptor.getInstallations()[0];
        final InstallSourceProperty installSourceProperty = nodejs.getProperties().get(InstallSourceProperty.class);
        final NodeJSInstaller nodeJSInstaller = installSourceProperty.installers.get(NodeJSInstaller.class);
        assertEquals("12.11.1", nodeJSInstaller.id);
        assertEquals(48, nodeJSInstaller.getNpmPackagesRefreshHours().longValue());
    }
}
