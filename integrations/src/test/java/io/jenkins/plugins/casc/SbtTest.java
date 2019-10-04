package io.jenkins.plugins.casc;

import hudson.ExtensionList;
import hudson.tools.InstallSourceProperty;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import io.jenkins.plugins.casc.model.CNode;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.plugins.SbtPluginBuilder;
import org.jvnet.hudson.plugins.SbtPluginBuilder.SbtInstallation;
import org.jvnet.hudson.plugins.SbtPluginBuilder.SbtInstaller;

import static io.jenkins.plugins.casc.misc.Util.getToolRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author v1v (Victor Martinez)
 */
public class SbtTest {

    @ClassRule
    @ConfiguredWithReadme("sbt/README.md")
    public static JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    public void configure_sbt_tool() {
        final SbtPluginBuilder.DescriptorImpl descriptor = ExtensionList.lookupSingleton(SbtPluginBuilder.DescriptorImpl.class);
        assertEquals(1, descriptor.getInstallations().length);

        SbtInstallation sbt = descriptor.getInstallations()[0];
        assertEquals("sbt", sbt.getName());
        assertEquals("/usr/bin/sbt", sbt.getHome());

        InstallSourceProperty installSourceProperty = sbt.getProperties().get(InstallSourceProperty.class);
        assertEquals(1, installSourceProperty.installers.size());

        SbtInstaller installer = installSourceProperty.installers.get(SbtInstaller.class);
        assertEquals("1.2.8", installer.id);
    }

    @Test
    public void export_sbt_tool() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getToolRoot(context).get("sbtInstallation");

        String exported = toYamlString(yourAttribute);

        String expected = toStringFromYamlFile(this, "SbtTestExpected.yml");

        assertThat(exported, is(expected));
    }
}
