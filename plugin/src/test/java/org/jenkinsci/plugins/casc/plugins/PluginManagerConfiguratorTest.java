package org.jenkinsci.plugins.casc.plugins;

import hudson.Plugin;
import org.jenkinsci.plugins.casc.ConfigurationAsCode;
import org.jenkinsci.plugins.casc.ConfigurationContext;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.jenkinsci.plugins.casc.model.Sequence;
import org.junit.*;
import org.yaml.snakeyaml.nodes.Node;

import java.io.IOException;
import java.io.StringWriter;

import static org.jenkinsci.plugins.casc.ConfigurationAsCode.serializeYamlNode;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class PluginManagerConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    public final ConfigurationContext context = new ConfigurationContext();

    @Test
    @Ignore //TODO: This needs to be re-enabled once we can actually dynamically load plugins
    @ConfiguredWithCode("PluginManagerConfiguratorTest.yml")
    public void testInstallPlugins() throws Exception {
        final Plugin chucknorris = j.jenkins.getPlugin("chucknorris");
        assertNotNull(chucknorris);
        assertEquals("1.0", chucknorris.getWrapper().getVersion());
    }

    @Test
    public void describeDefaultConfig() throws Exception {
        final PluginManagerConfigurator root = getPluginManagerConfigurator();
        final CNode node = root.describe(root.getTargetComponent(context));
        assertNotNull(node);
        assertTrue(node instanceof Mapping);
        final Object sites = ((Mapping) node).get("sites");
        assertNotNull(sites);
        assertTrue(sites instanceof Sequence);
        assertEquals(1, ((Sequence) sites).size());
        final Object site = ((Sequence) sites).get(0);
        assertNotNull(site);
        assertTrue(site instanceof Mapping);
        assertEquals("default", ((Mapping) site).get("id").toString());
        assertTrue(((Mapping) site).containsKey("url"));
    }

    @Test
    @ConfiguredWithCode("ProxyConfigTest.yml")
    public void describeProxyConfig() throws Exception {
        final PluginManagerConfigurator root = getPluginManagerConfigurator();
        final CNode configNode = root.describe(root.getTargetComponent(context));
        ((Mapping) configNode).remove("sites");
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

    private PluginManagerConfigurator getPluginManagerConfigurator() {
        return j.jenkins.getExtensionList(PluginManagerConfigurator.class).get(0);
    }

    private static String toYamlString(CNode rootNode) throws IOException {
        Node yamlRoot = ConfigurationAsCode.get().toYaml(rootNode);
        StringWriter buffer = new StringWriter();
        serializeYamlNode(yamlRoot, buffer);
        return buffer.toString();
    }
}
