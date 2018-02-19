package org.jenkinsci.plugins.casc;

import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;

/**
 * Created by mads on 1/31/18.
 */
public class PluginConfiguratorTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule();

    @Test
    public void test_that_proxy_is_configured() throws Exception {
        ConfigurationAsCode.configure(getClass().getResourceAsStream("PluginTest.yml"));
        assertEquals("proxy.acme.com", Jenkins.getInstance().proxy.name);
        assertEquals(9090, Jenkins.getInstance().proxy.port);
        assertEquals("bar", Jenkins.getInstance().proxy.getPassword());
        assertEquals(2, Jenkins.getInstance().getUpdateCenter().getSiteList().size());
    }

}
