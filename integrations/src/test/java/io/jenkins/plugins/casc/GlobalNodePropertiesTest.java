package io.jenkins.plugins.casc;

import static io.jenkins.plugins.casc.misc.Util.getJenkinsRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.node_monitors.DiskSpaceMonitorNodeProperty;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.tools.ToolLocationNodeProperty;
import hudson.util.DescribableList;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import io.jenkins.plugins.casc.model.CNode;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
class GlobalNodePropertiesTest {

    @Test
    @ConfiguredWithCode("GlobalNodePropertiesTest.yml")
    void configure(JenkinsConfiguredWithCodeRule j) {
        final Jenkins jenkins = Jenkins.get();

        DescribableList<NodeProperty<?>, NodePropertyDescriptor> nodeProperties = jenkins.getGlobalNodeProperties();

        assertEquals(3, nodeProperties.size());

        Set<Map.Entry<String, String>> envVars = ((EnvironmentVariablesNodeProperty)
                        nodeProperties.get(EnvironmentVariablesNodeProperty.class))
                .getEnvVars()
                .entrySet();
        assertEquals(3, envVars.size());

        Iterator<Entry<String, String>> iterator = envVars.iterator();
        Map.Entry<String, String> envVar = iterator.next();
        assertEquals("FOO", envVar.getKey());
        assertEquals("BAR", envVar.getValue());

        envVar = iterator.next();
        assertEquals("FOO2", envVar.getKey());
        assertEquals("", envVar.getValue());

        envVar = iterator.next();
        assertEquals("FOO3", envVar.getKey());
        assertEquals("", envVar.getValue());

        DiskSpaceMonitorNodeProperty diskSpace = nodeProperties.get(DiskSpaceMonitorNodeProperty.class);
        assertEquals("1GiB", diskSpace.getFreeDiskSpaceThreshold());
        assertEquals("2GiB", diskSpace.getFreeDiskSpaceWarningThreshold());
        assertEquals("1GiB", diskSpace.getFreeTempSpaceThreshold());
        assertEquals("2GiB", diskSpace.getFreeTempSpaceWarningThreshold());

        ToolLocationNodeProperty toolLocations = nodeProperties.get(ToolLocationNodeProperty.class);
        assertEquals(1, toolLocations.getLocations().size());
        assertEquals("Default", toolLocations.getLocations().get(0).getName());
        assertEquals("/home/user/bin/git", toolLocations.getLocations().get(0).getHome());
    }

    @Test
    @ConfiguredWithCode("GlobalNodePropertiesTest.yml")
    void export(JenkinsConfiguredWithCodeRule j) throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getJenkinsRoot(context).get("globalNodeProperties");

        String exported = toYamlString(yourAttribute);

        String expected = toStringFromYamlFile(this, "GlobalNodePropertiesTestExpected.yml");

        assertThat(exported, is(expected));
    }
}
