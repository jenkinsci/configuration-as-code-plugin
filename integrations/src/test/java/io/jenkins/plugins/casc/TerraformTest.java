package io.jenkins.plugins.casc;

import hudson.ExtensionList;
import hudson.tools.InstallSourceProperty;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import io.jenkins.plugins.casc.model.CNode;
import org.jenkinsci.plugins.terraform.TerraformInstallation;
import org.jenkinsci.plugins.terraform.TerraformInstaller;
import org.junit.ClassRule;
import org.junit.Test;

import static io.jenkins.plugins.casc.misc.Util.getToolRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;


/**
 * @author v1v (Victor Martinez)
 */
public class TerraformTest {

    @ClassRule
    @ConfiguredWithReadme("terraform/README.md")
    public static JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    public void configure_terraform_tool() {
        final TerraformInstallation.DescriptorImpl descriptor = ExtensionList.lookupSingleton(TerraformInstallation.DescriptorImpl.class);
        assertEquals(1, descriptor.getInstallations().length);

        TerraformInstallation terraform = descriptor.getInstallations()[0];
        assertEquals("terraform", terraform.getName());
        assertEquals("/terraform-0.11", terraform.getHome());

        InstallSourceProperty installSourceProperty = terraform.getProperties().get(InstallSourceProperty.class);
        assertEquals(1, installSourceProperty.installers.size());

        TerraformInstaller installer = installSourceProperty.installers.get(TerraformInstaller.class);
        assertEquals("0.11.9-linux-amd64", installer.id);
    }

    @Test
    public void export_terraform_tool() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getToolRoot(context).get("terraform");

        String exported = toYamlString(yourAttribute);

        String expected = toStringFromYamlFile(this, "TerraformConfiguratorTestExpected.yml");

        assertThat(exported, is(expected));
    }
}
