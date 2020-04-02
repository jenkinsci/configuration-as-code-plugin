package io.jenkins.plugins.casc;

import hudson.ProxyConfiguration;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.jvnet.hudson.test.JenkinsMatchers.hasPlainText;

/**
 * @author v1v (Victor Martinez)
 */
public class ProxyTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("proxy/README.md")
    public void configure_proxy() {
        final ProxyConfiguration proxy = Jenkins.get().proxy;
        assertNotNull(proxy);
        assertThat(proxy.getSecretPassword(), hasPlainText("password"));
        assertThat(proxy.getTestUrl(), is("http://google.com"));
        assertThat(proxy.getUserName(), is("login"));
        assertThat(proxy.name, is("proxyhost"));
        assertThat(proxy.noProxyHost, is("externalhost"));
        assertThat(proxy.port, is(80));
    }
}
