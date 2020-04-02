package io.jenkins.plugins.casc;

import hudson.ExtensionList;
import hudson.plugins.msbuild.MsBuildInstallation;
import hudson.plugins.msbuild.MsBuildInstallation.DescriptorImpl;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MSBuildTest {
    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("msbuild/README.md")
    public void configure_msbuild() {
        final DescriptorImpl msBuildDescriptor = ExtensionList.lookupSingleton(DescriptorImpl.class);
        assertNotNull(msBuildDescriptor);
        assertEquals(1, msBuildDescriptor.getInstallations().length);

        final MsBuildInstallation msBuildInstallation = msBuildDescriptor.getInstallations()[0];
        assertEquals("MSBuild Latest", msBuildInstallation.getName());
        assertEquals("C:\\WINDOWS\\Microsoft.NET\\Framework\\14.0\\Bin\\MSBuild.exe", msBuildInstallation.getHome());
        assertEquals("/p:Configuration=Debug", msBuildInstallation.getDefaultArgs());
    }
}
