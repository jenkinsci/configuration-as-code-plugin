package org.jenkinsci.plugins.casc;

import hudson.Plugin;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.jenkinsci.plugins.casc.model.Sequence;
import org.jenkinsci.plugins.casc.plugins.PluginManagerConfigurator;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class PluginManagerConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("PluginManagerConfiguratorTest.yml")
    public void testInstallPlugins() {
        final Plugin chucknorris = j.jenkins.getPlugin("chucknorris");
        assertNotNull(chucknorris);
        assertEquals("1.0", chucknorris.getWrapper().getVersion());
    }

    @Test
    public void describeConfig() throws Exception {
        final PluginManagerConfigurator root = j.jenkins.getExtensionList(PluginManagerConfigurator.class).get(0);
        final CNode node = root.describe(root.getTargetComponent());
        assertNotNull(node);
        assertTrue(node instanceof Mapping);
        final Object sites = ((Mapping) node).get("sites");
        assertNotNull(sites);
        assertTrue(sites instanceof Sequence);
        assertEquals(1, ((Sequence)sites).size());
        final Object site = ((Sequence)sites).get(0);
        assertNotNull(site);
        assertTrue(site instanceof Mapping);
        assertEquals("default", ((Mapping)site).get("id").toString());
        assertTrue(((Mapping)site).containsKey("url"));
    }
}
