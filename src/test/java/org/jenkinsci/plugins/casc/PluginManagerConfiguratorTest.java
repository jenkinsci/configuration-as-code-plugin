package org.jenkinsci.plugins.casc;

import hudson.Plugin;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class PluginManagerConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("PluginManagerConfiguratorTest.yml")
    public void testInstallPlugins() {
        final Plugin git = j.jenkins.getPlugin("git");
        assertNotNull(git);
        assertEquals("3.8.0", git.getWrapper().getVersion());
    }
}
