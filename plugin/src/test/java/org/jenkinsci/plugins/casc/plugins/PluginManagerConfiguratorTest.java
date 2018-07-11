/*
 * Copyright (c) 2018 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.casc.plugins;

import hudson.Plugin;
import org.jenkinsci.plugins.casc.ConfigurationAsCode;
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
        final CNode node = root.describe(root.getTargetComponent());
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
        final CNode configNode = root.describe(root.getTargetComponent());
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
