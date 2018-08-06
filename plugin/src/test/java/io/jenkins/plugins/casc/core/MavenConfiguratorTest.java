package io.jenkins.plugins.casc.core;

import hudson.tasks.Maven;
import hudson.tools.InstallSourceProperty;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import jenkins.mvn.FilePathSettingsProvider;
import jenkins.mvn.GlobalMavenConfig;
import jenkins.mvn.SettingsProvider;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class MavenConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("MavenConfiguratorTest.yml")
    public void should_configure_maven_tools_and_global_config() {
        final Maven.DescriptorImpl descriptor= (Maven.DescriptorImpl) j.jenkins.getDescriptorOrDie(Maven.class);
        Assert.assertEquals(1, descriptor.getInstallations().length);
        Assert.assertEquals("/usr/share/maven", descriptor.getInstallations()[0].getHome());

        InstallSourceProperty installSourceProperty = descriptor.getInstallations()[0].getProperties().get(InstallSourceProperty.class);
        Assert.assertEquals("3.5.0", installSourceProperty.installers.get(Maven.MavenInstaller.class).id);

        final SettingsProvider provider = GlobalMavenConfig.get().getSettingsProvider();
        Assert.assertTrue(provider instanceof FilePathSettingsProvider);
        Assert.assertEquals("/usr/share/maven-settings.xml", ((FilePathSettingsProvider)provider).getPath());
    }
}
