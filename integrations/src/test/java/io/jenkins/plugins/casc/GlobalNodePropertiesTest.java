package io.jenkins.plugins.casc;

import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import java.util.Map;
import java.util.Set;
import jenkins.model.Jenkins;
import org.junit.ClassRule;
import org.junit.Test;

import static io.jenkins.plugins.casc.misc.Util.getJenkinsRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;


public class GlobalNodePropertiesTest {

    @ClassRule
    @ConfiguredWithCode("GlobalNodePropertiesTest.yml")
    public static JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void configure() {
        final Jenkins jenkins = Jenkins.get();

        DescribableList<NodeProperty<?>, NodePropertyDescriptor> nodeProperties = jenkins.getGlobalNodeProperties();

        Set<Map.Entry<String, String>> entries = ((EnvironmentVariablesNodeProperty) nodeProperties.get(0)).getEnvVars().entrySet();
        assertEquals(1, entries.size());

        Map.Entry<String, String> envVar = entries.iterator().next();
        assertEquals("FOO", envVar.getKey());
        assertEquals("BAR", envVar.getValue());
    }

    @Test
    public void export() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getJenkinsRoot(context).get("globalNodeProperties");

        String exported = toYamlString(yourAttribute);

        String expected = toStringFromYamlFile(this, "GlobalNodePropertiesTestExpected.yml");

        assertThat(exported, is(expected));
    }
}
