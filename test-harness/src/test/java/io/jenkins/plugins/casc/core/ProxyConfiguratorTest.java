package io.jenkins.plugins.casc.core;

import hudson.ProxyConfiguration;
import hudson.util.Secret;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.Env;
import io.jenkins.plugins.casc.misc.EnvVarsRule;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.Util;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.LoggerRule;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ProxyConfiguratorTest {

    final JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();
    public LoggerRule logging = new LoggerRule();

    @Rule
    public RuleChain chain = RuleChain
            .outerRule(logging.record(Logger.getLogger(Attribute.class.getName()), Level.FINER).capture(2048))
            .around(new EnvVarsRule())
            .around(j);

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
    @Env(name = "PROXY_HOST", value = "proxyhost")
    @Env(name = "PROXY_PORT", value = "80")
    @Env(name = "PROXY_USER", value = "proxy_user")
    @Env(name = "PROXY_PASSWORD", value = "proxy_password")
    @Env(name = "PROXY_NOPROXY", value = "external.host")
    @Env(name = "PROXY_TEST_URL", value = "http://google.com")
    @ConfiguredWithCode("ProxyWithSecrets.yml")
    public void shouldSetProxyWithSecretInFields() {
        ProxyConfiguration proxy = j.jenkins.proxy;
        assertEquals(proxy.name, "proxyhost");
        assertEquals(proxy.port, 80);

        assertEquals(proxy.getUserName(), "proxy_user");
        assertEquals(Secret.decrypt(proxy.getEncryptedPassword()).getPlainText(), "proxy_password");
        assertEquals(proxy.noProxyHost, "external.host");
        assertEquals(proxy.getTestUrl(), "http://google.com");
    }

    @Test
    @Env(name = "PROXY_USER", value = "proxy_user")
    @Env(name = "PROXY_PASSWORD", value = "proxy_password")
    @ConfiguredWithCode("ProxyWithSecrets.yml")
    @Issue("SECURITY-1303") // Fixed in 1.20
    public void shouldNotWritePasswordToLog() {
        ProxyConfiguration proxy = j.jenkins.proxy;
        assertEquals(proxy.getUserName(), "proxy_user");
        assertEquals(Secret.decrypt(proxy.getEncryptedPassword()).getPlainText(), "proxy_password");

        // Check logs
        Util.assertLogContains(logging, "password");
        Util.assertNotInLog(logging, "proxy_password");
    }

    @Test
    @ConfiguredWithCode("Proxy.yml")
    public void describeProxyConfig() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final CNode configNode = getProxyNode(context);

        Secret password = requireNonNull(Secret.decrypt(getProxyNode(context).getScalarValue("password")));

        final String yamlConfig = Util.toYamlString(configNode);
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
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final CNode configNode = getProxyNode(context);

        Secret password = requireNonNull(Secret.decrypt(getProxyNode(context).getScalarValue("password")));

        final String yamlConfig = Util.toYamlString(configNode);
        assertEquals(String.join("\n",
                "name: \"proxyhost\"",
                "password: \"" + password.getEncryptedValue() + "\"", // It's an empty string here
                "port: 80",
                ""
        ), yamlConfig);
    }

    private Mapping getProxyNode(ConfigurationContext context) throws Exception {
        return Util.getJenkinsRoot(context).get("proxy").asMapping();
    }
}
