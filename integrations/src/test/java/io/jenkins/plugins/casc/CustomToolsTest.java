package io.jenkins.plugins.casc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.cloudbees.jenkins.plugins.customtools.CustomTool;
import com.cloudbees.jenkins.plugins.customtools.CustomTool.DescriptorImpl;
import hudson.tools.CommandInstaller;
import hudson.tools.InstallSourceProperty;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@WithJenkinsConfiguredWithCode
class CustomToolsTest {

    @Test
    @Issue("#97")
    @Disabled
    @ConfiguredWithCode(value = "CustomToolsTest.yml")
    void configure_custom_tools(JenkinsConfiguredWithCodeRule j) {
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
