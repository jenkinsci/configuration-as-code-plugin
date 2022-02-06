package io.jenkins.plugins.casc;

import hudson.ExtensionList;
import hudson.tasks.Maven;
import hudson.tasks.Maven.MavenInstaller;
import hudson.tools.InstallSourceProperty;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import io.jenkins.plugins.casc.model.CNode;
import jenkins.mvn.DefaultGlobalSettingsProvider;
import jenkins.mvn.DefaultSettingsProvider;
import jenkins.mvn.FilePathGlobalSettingsProvider;
import jenkins.mvn.FilePathSettingsProvider;
import jenkins.mvn.GlobalMavenConfig;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import static io.jenkins.plugins.casc.misc.Util.getToolRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;

@Issue("JENKINS-62446")
public class MavenConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("maven/README.md#0")
    public void configure_maven_tool() {
        final Maven.DescriptorImpl descriptor = ExtensionList.lookupSingleton(Maven.DescriptorImpl.class);
        assertEquals(1, descriptor.getInstallations().length);

        Maven.MavenInstallation maven = descriptor.getInstallations()[0];
        assertEquals("maven3", maven.getName());
        assertEquals("/maven3", maven.getHome());

        InstallSourceProperty installSourceProperty = maven.getProperties().get(InstallSourceProperty.class);
        assertEquals(1, installSourceProperty.installers.size());

        MavenInstaller installer = installSourceProperty.installers.get(MavenInstaller.class);
        assertEquals("3.8.4", installer.id);
    }

    @Test
    @ConfiguredWithReadme("maven/README.md#0")
    public void export_maven_tool() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getToolRoot(context).get("maven");

        String exported = toYamlString(yourAttribute);

        String expected = toStringFromYamlFile(this, "MavenConfiguratorTestExpected.yml");

        assertThat(exported, is(expected));
    }

    @Test
    @ConfiguredWithReadme("maven/README.md#1")
    public void configure_maven_global_config() {
        final GlobalMavenConfig descriptor = ExtensionList.lookupSingleton(GlobalMavenConfig.class);
        assertThat(descriptor.getGlobalSettingsProvider(), instanceOf(DefaultGlobalSettingsProvider.class));
        assertThat(descriptor.getSettingsProvider(), instanceOf(DefaultSettingsProvider.class));
    }

    @Test
    @ConfiguredWithReadme("maven/README.md#1")
    public void export_maven_global_config() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getToolRoot(context).get("mavenGlobalConfig");

        String exported = toYamlString(yourAttribute);

        String expected = toStringFromYamlFile(this, "MavenConfiguratorTestGlobalConfigExpected.yml");

        assertThat(exported, is(expected));
    }

    @Test
    @ConfiguredWithReadme("maven/README.md#2")
    public void configure_maven_global_config_specific_files() {
        final GlobalMavenConfig descriptor = ExtensionList.lookupSingleton(GlobalMavenConfig.class);

        assertThat(descriptor.getGlobalSettingsProvider(), instanceOf(FilePathGlobalSettingsProvider.class));
        FilePathGlobalSettingsProvider globalProvider = (FilePathGlobalSettingsProvider) descriptor.getGlobalSettingsProvider();
        assertThat(globalProvider.getPath(), is("/conf/maven/global-settings.xml"));

        assertThat(descriptor.getSettingsProvider(), instanceOf(FilePathSettingsProvider.class));
        FilePathSettingsProvider provider = (FilePathSettingsProvider) descriptor.getSettingsProvider();
        assertThat(provider.getPath(), is("/conf/maven/settings.xml"));
    }

    @Test
    @ConfiguredWithReadme("maven/README.md#2")
    public void export_maven_global_config_specific_files() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getToolRoot(context).get("mavenGlobalConfig");

        String exported = toYamlString(yourAttribute);

        String expected = toStringFromYamlFile(this, "MavenConfiguratorTestGlobalConfigSpecificFilesExpected.yml");

        assertThat(exported, is(expected));
    }
}
