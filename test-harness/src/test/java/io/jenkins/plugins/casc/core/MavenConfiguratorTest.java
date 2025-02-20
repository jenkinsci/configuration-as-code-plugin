package io.jenkins.plugins.casc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import hudson.tasks.Maven;
import hudson.tools.InstallSourceProperty;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import jenkins.mvn.FilePathSettingsProvider;
import jenkins.mvn.GlobalMavenConfig;
import jenkins.mvn.SettingsProvider;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@WithJenkinsConfiguredWithCode
class MavenConfiguratorTest {

    @Test
    @ConfiguredWithCode("MavenConfiguratorTest.yml")
    void should_configure_maven_tools_and_global_config(JenkinsConfiguredWithCodeRule j) {
        final Maven.DescriptorImpl descriptor = (Maven.DescriptorImpl) j.jenkins.getDescriptorOrDie(Maven.class);
        assertEquals(1, descriptor.getInstallations().length);
        assertEquals("/usr/share/maven", descriptor.getInstallations()[0].getHome());

        InstallSourceProperty installSourceProperty =
                descriptor.getInstallations()[0].getProperties().get(InstallSourceProperty.class);
        assertEquals("3.5.0", installSourceProperty.installers.get(Maven.MavenInstaller.class).id);

        final SettingsProvider provider = GlobalMavenConfig.get().getSettingsProvider();
        assertInstanceOf(FilePathSettingsProvider.class, provider);
        assertEquals("/usr/share/maven-settings.xml", ((FilePathSettingsProvider) provider).getPath());
    }
}
