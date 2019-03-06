package io.jenkins.plugins.casc.core;

import hudson.model.UpdateSite;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.snakeyaml.nodes.Node;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static io.jenkins.plugins.casc.ConfigurationAsCode.serializeYamlNode;
import static org.junit.Assert.assertEquals;

public class ProxyConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("Proxy.yml")
    public void shouldSetUpdateCenterSites() {
        List<UpdateSite> sites = j.jenkins.getUpdateCenter().getSites();
        assertEquals(2, sites.size());
        UpdateSite siteOne = sites.get(0);
        assertEquals("default", siteOne.getId());
        assertEquals("https://updates.jenkins.io/update-center.json", siteOne.getUrl());
        UpdateSite siteTwo = sites.get(1);
        assertEquals("experimental", siteTwo.getId());
        assertEquals("https://updates.jenkins.io/experimental/update-center.json", siteTwo.getUrl());
    }

    @Test
    @ConfiguredWithCode("Proxy.yml")
    public void describeProxyConfig() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();

        JenkinsConfigurator root = j.jenkins.getExtensionList(JenkinsConfigurator.class).get(0);
        ConfigurationContext context = new ConfigurationContext(registry);
        final CNode configNode = root.describe(root.getTargetComponent(context), context);
        final String yamlConfig = toYamlString(configNode);
        assertEquals(String.join("\n",
                "proxy:",
                "  name: \"proxyhost\"",
                "  noProxyHost: \"externalhost\"",
                "  password: \"password\"",
                "  port: 80",
                "  testUrl: \"http://google.com\"",
                "  userName: \"login\"",
                ""
        ), yamlConfig);
    }

    private static String toYamlString(CNode rootNode) throws IOException {
        Node yamlRoot = ConfigurationAsCode.get().toYaml(rootNode);
        StringWriter buffer = new StringWriter();
        serializeYamlNode(yamlRoot, buffer);
        return buffer.toString();
    }
}
