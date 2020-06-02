package io.jenkins.plugins.casc;

import hudson.ExtensionList;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import org.jenkinsci.plugins.MsTestInstallation;
import org.jenkinsci.plugins.MsTestInstallation.DescriptorImpl;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MSTestRunnerTest {
    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("mstestrunner/README.md")
    public void configure_mstestrunner() {
        final DescriptorImpl msTestRunnerDescriptor = ExtensionList.lookupSingleton(DescriptorImpl.class);
        assertNotNull(msTestRunnerDescriptor);
        assertEquals(1, msTestRunnerDescriptor.getInstallations().length);

        final MsTestInstallation msTestRunnerInstallation = msTestRunnerDescriptor.getInstallations()[0];
        assertEquals("MSTest test", msTestRunnerInstallation.getName());
        assertEquals("C:\\Program Files (x86)\\Microsoft Visual Studio 10.0\\Common7\\IDE\\MSTest.exe", msTestRunnerInstallation.getHome());
        assertEquals("/category:SmokeTests", msTestRunnerInstallation.getDefaultArgs());
        assertTrue(msTestRunnerInstallation.getOmitNoIsolation());
    }
}
