package io.jenkins.plugins.casc.core;

import hudson.ProxyConfiguration;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ProxyConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("Proxy.yml")
    public void shouldSetProxyWithAllFields() {
        ProxyConfiguration proxy = j.jenkins.proxy;
        assertEquals(proxy.name, "proxyhost");
        assertEquals(proxy.port, 80);

        assertEquals(proxy.getUserName(), "login");
        assertEquals(proxy.getPassword(), "password");
        assertEquals(proxy.noProxyHost, "externalhost");
        assertEquals(proxy.getTestUrl(), "http://google.com");
    }

    @Test
    @ConfiguredWithCode("ProxyMinimal.yml")
    public void shouldSetProxyWithMinimumFields() {
        ProxyConfiguration proxy = j.jenkins.proxy;
        assertEquals(proxy.name, "proxyhost");
        assertEquals(proxy.port, 80);

        assertNull(proxy.getUserName());
        assertNull(proxy.getTestUrl());
    }
}
