package io.jenkins.plugins.casc.core;

import hudson.model.JDK;
import hudson.tasks.Maven;
import hudson.tools.InstallSourceProperty;
import hudson.tools.JDKInstaller;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.mvn.FilePathSettingsProvider;
import jenkins.mvn.GlobalMavenConfig;
import jenkins.mvn.SettingsProvider;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author <a href="mailto:vektory79@gmail.com">Viktor Verbitsky</a>
 */
public class JdkConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("JdkConfiguratorTest.yml")
    public void should_configure_maven_tools_and_global_config() {
        final Object descriptor = j.jenkins.getDescriptorOrDie(JDK.class);
        Assert.assertNotNull(descriptor);
        Assert.assertEquals(1, ((JDK.DescriptorImpl) descriptor).getInstallations().length);

        JDK jdk = ((JDK.DescriptorImpl) descriptor).getInstallations()[0];
        Assert.assertEquals("jdk8", jdk.getName());
        Assert.assertEquals("/jdk", jdk.getHome());

        InstallSourceProperty installSourceProperty = jdk.getProperties().get(InstallSourceProperty.class);
        Assert.assertEquals(1, installSourceProperty.installers.size());

        JDKInstaller installer = installSourceProperty.installers.get(JDKInstaller.class);
        Assert.assertEquals("jdk-8u181-oth-JPR", installer.id);
        Assert.assertTrue(installer.acceptLicense);
    }
}
