package org.jenkinsci.plugins.casc;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Created by mads on 2/14/18.
 */
public class JenkinsPluginInstallTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void checkThatWeCanInstallPlugins() throws Exception {
        //TODO: I'm not sure on how to check what plugins actually was installed. When i do
        //Jenkins.getInstance().getPlugin('git') i always get
        //null. But in the log it correctly tells me it's installing the requested plugins
        ConfigurationAsCode.configure(getClass().getResourceAsStream("PluginsTest.yml"));
    }

}
