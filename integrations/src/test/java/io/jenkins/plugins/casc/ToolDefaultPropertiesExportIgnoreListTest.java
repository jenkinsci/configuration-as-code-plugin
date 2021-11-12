package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import static io.jenkins.plugins.casc.misc.Util.getToolRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;


public class ToolDefaultPropertiesExportIgnoreListTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @Issue("JENKINS-57122")
    @ConfiguredWithCode("ToolDefaultPropertiesExportIgnoreList.yml")
    public void export_tool_configuration() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getToolRoot(context);

        String exported = toYamlString(yourAttribute).replaceAll("git\\.exe", "git");

        String expected = toStringFromYamlFile(this, "ToolDefaultPropertiesExportIgnoreListExpected.yml");

        assertThat(exported, is(expected));
    }
}
