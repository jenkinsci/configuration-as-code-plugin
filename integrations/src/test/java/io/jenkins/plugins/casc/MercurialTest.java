package io.jenkins.plugins.casc;

import hudson.model.JDK;
import hudson.plugins.mercurial.MercurialInstallation;
import hudson.tools.CommandInstaller;
import hudson.tools.InstallSourceProperty;
import hudson.tools.JDKInstaller;
import io.jenkins.plugins.casc.core.MavenConfigurator;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author <a href="mailto:vektory79@gmail.com">Viktor Verbitsky</a>
 */
public class MercurialTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("MercurialTest.yaml")
    public void should_configure_maven_tools_and_global_config() {
        final Object descriptor = j.jenkins.getDescriptorOrDie(MercurialInstallation.class);
        Assert.assertNotNull(descriptor);
        Assert.assertEquals(1, ((MercurialInstallation.DescriptorImpl) descriptor).getInstallations().length);

        MercurialInstallation mercurial = ((MercurialInstallation.DescriptorImpl) descriptor).getInstallations()[0];
        Assert.assertEquals("Mercurial 3", mercurial.getName());
        Assert.assertEquals("/mercurial", mercurial.getHome());
        Assert.assertEquals("[defaults]\n" +
                "clone = --uncompressed\n" +
                "bundle = --type none", mercurial.getConfig());
        Assert.assertEquals("INSTALLATION/bin/hg", mercurial.getExecutable());
        Assert.assertTrue(mercurial.isUseCaches());
        Assert.assertFalse(mercurial.getDebug());
        Assert.assertFalse(mercurial.getDebug());
        Assert.assertEquals("/cache/root", mercurial.getMasterCacheRoot());
        Assert.assertFalse(mercurial.isUseSharing());

        InstallSourceProperty installSourceProperty = mercurial.getProperties().get(InstallSourceProperty.class);
        Assert.assertEquals(1, installSourceProperty.installers.size());

        CommandInstaller installer = installSourceProperty.installers.get(CommandInstaller.class);
        Assert.assertEquals("mercurial", installer.getToolHome());
        Assert.assertEquals("SomeLabel", installer.getLabel());
        Assert.assertEquals("[ -d mercurial ] || wget -q -O - http://www.archlinux.org/packages/extra/x86_64/mercurial/download/ | xzcat | tar xvf -", installer.getCommand());
    }
}
