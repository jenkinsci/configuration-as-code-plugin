package io.jenkins.plugins.casc;

import com.cloudbees.jenkins.plugins.customtools.CustomTool;
import com.cloudbees.jenkins.plugins.customtools.CustomTool.DescriptorImpl;
import hudson.tools.CommandInstaller;
import hudson.tools.InstallSourceProperty;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class CustomToolsTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test @Issue("#97") @Ignore
    @ConfiguredWithCode(value = "CustomToolsTest.yml")
    public void configure_custom_tools() throws Exception {
        DescriptorImpl descriptor = (DescriptorImpl) j.jenkins.getDescriptorOrDie(CustomTool.class);
        assertEquals(1, descriptor.getInstallations().length);
        final CustomTool customTool = descriptor.getInstallations()[0];
        final InstallSourceProperty source = customTool.getProperties().get(InstallSourceProperty.class);
        assertNotNull(source);
        final CommandInstaller installer = source.installers.get(CommandInstaller.class);
        assertNotNull(installer);
        assertEquals("/bin/my-tool", installer.getToolHome());
    }
}
