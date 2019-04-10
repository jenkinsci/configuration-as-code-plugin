package io.jenkins.plugins.casc.core;

import hudson.ProxyConfiguration;
import hudson.util.Secret;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.snakeyaml.nodes.Node;
import java.io.IOException;
import java.io.StringWriter;
import org.junit.Rule;
import org.junit.Test;

import static io.jenkins.plugins.casc.ConfigurationAsCode.serializeYamlNode;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ProxyConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("Proxy.yml")
    public void shouldSetProxyWithAllFields() throws Exception {
        ProxyConfiguration proxy = j.jenkins.proxy;
        assertEquals(proxy.name, "proxyhost");
        assertEquals(proxy.port, 80);

        assertEquals(proxy.getUserName(), "login");
        assertEquals(Secret.decrypt(proxy.getEncryptedPassword()).getPlainText(), "password");
        assertEquals(proxy.noProxyHost, "externalhost");
        assertEquals(proxy.getTestUrl(), "http://google.com");

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final Configurator c = context.lookupOrFail(ProxyConfiguration.class);
        final CNode node = c.describe(proxy, context);
        assertNotNull(node);
        Mapping mapping = node.asMapping();
        assertEquals(6, mapping.size());
        assertEquals("proxyhost", mapping.getScalarValue("name"));
    }

    @Test
    @ConfiguredWithCode("ProxyMinimal.yml")
    public void shouldSetProxyWithMinimumFields() throws Exception {
        ProxyConfiguration proxy = j.jenkins.proxy;
        assertEquals(proxy.name, "proxyhost");
        assertEquals(proxy.port, 80);

        assertNull(proxy.getUserName());
        assertNull(proxy.getTestUrl());

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final Configurator c = context.lookupOrFail(ProxyConfiguration.class);
        final CNode node = c.describe(proxy, context);
        assertNotNull(node);
        Mapping mapping = node.asMapping();
        assertEquals(3, node.asMapping().size());
        assertEquals("proxyhost", mapping.getScalarValue("name"));
        assertEquals("", Secret.decrypt(mapping.getScalarValue("password")).getPlainText());
    }

    @Test
    @ConfiguredWithCode("Proxy.yml")
    public void describeProxyConfig() throws Exception {
        final JenkinsConfigurator root = getJenkinsConfigurator();
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final CNode configNode = getProxyNode(root, context);

        Secret password = requireNonNull(Secret.decrypt(getProxyNode(root, context).asMapping().getScalarValue("password")));

        final String yamlConfig = toYamlString(configNode);
        assertEquals(String.join("\n",
                "name: \"proxyhost\"",
                "noProxyHost: \"externalhost\"",
                "password: \"" + password.getEncryptedValue() + "\"",
                "port: 80",
                "testUrl: \"http://google.com\"",
                "userName: \"login\"",
                ""
        ), yamlConfig);
    }

    @Test
    @ConfiguredWithCode("ProxyMinimal.yml")
    public void describeMinimalProxyConfig() throws Exception {
        final JenkinsConfigurator root = getJenkinsConfigurator();
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final CNode configNode = getProxyNode(root, context);

        Secret password = requireNonNull(Secret.decrypt(getProxyNode(root, context).asMapping().getScalarValue("password")));

        final String yamlConfig = toYamlString(configNode);
        assertEquals(String.join("\n",
                "name: \"proxyhost\"",
                "password: \"" + password.getEncryptedValue() + "\"", // It's an empty string here
                "port: 80",
                ""
        ), yamlConfig);
    }

    private CNode getProxyNode(JenkinsConfigurator root, ConfigurationContext context) throws Exception {
        return requireNonNull(root.describe(root.getTargetComponent(context), context)).asMapping().get("proxy");
    }

    private JenkinsConfigurator getJenkinsConfigurator() {
        return j.jenkins.getExtensionList(JenkinsConfigurator.class).get(0);
    }

    private static String toYamlString(CNode rootNode) throws IOException {
        Node yamlRoot = ConfigurationAsCode.get().toYaml(rootNode);
        StringWriter buffer = new StringWriter();
        serializeYamlNode(yamlRoot, buffer);
        return buffer.toString();
    }
}
