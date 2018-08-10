package io.jenkins.plugins.casc;

import com.cloudbees.jenkins.plugins.customtools.CustomTool;
import com.cloudbees.jenkins.plugins.customtools.CustomTool.DescriptorImpl;
import hudson.plugins.active_directory.ActiveDirectoryDomain;
import hudson.plugins.active_directory.ActiveDirectorySecurityRealm;
import hudson.tools.CommandInstaller;
import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolInstaller;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.EnvVarsRule;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.Issue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class CustomToolsTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test @Issue("#97")
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
