package io.jenkins.plugins.casc;

import static io.jenkins.plugins.casc.misc.Util.getToolRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;

import hudson.ExtensionList;
import hudson.model.JDK;
import hudson.tools.InstallSourceProperty;
import io.jenkins.plugins.adoptopenjdk.AdoptOpenJDKInstaller;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import io.jenkins.plugins.casc.model.CNode;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author <a href="mailto:vektory79@gmail.com">Viktor Verbitsky</a>
 */
public class JdkConfiguratorTest {

    @ClassRule
    @ConfiguredWithReadme("jdk/README.md")
    public static JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    public void configure_jdk_tool() {
        final JDK.DescriptorImpl descriptor = ExtensionList.lookupSingleton(JDK.DescriptorImpl.class);
        assertEquals(1, descriptor.getInstallations().length);

        JDK jdk = descriptor.getInstallations()[0];
        assertEquals("jdk11", jdk.getName());
        assertEquals("/jdk", jdk.getHome());

        InstallSourceProperty installSourceProperty = jdk.getProperties().get(InstallSourceProperty.class);
        assertEquals(1, installSourceProperty.installers.size());

        AdoptOpenJDKInstaller installer = installSourceProperty.installers.get(AdoptOpenJDKInstaller.class);
        assertEquals("jdk-11.0.14+9", installer.id);
    }

    @Test
    public void export_jdk_tool() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getToolRoot(context).get("jdk");

        String exported = toYamlString(yourAttribute);

        String expected = toStringFromYamlFile(this, "JdkConfiguratorTestExpected.yml");

        assertThat(exported, is(expected));
    }
}
