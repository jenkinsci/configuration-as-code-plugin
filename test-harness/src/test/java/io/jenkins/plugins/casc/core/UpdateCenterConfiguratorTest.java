package io.jenkins.plugins.casc.core;

import hudson.model.UpdateCenter;
import hudson.model.UpdateSite;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class UpdateCenterConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("UpdateCenter.yml")
    public void shouldSetUpdateCenterSites() throws Exception {
        UpdateCenter updateCenter = j.jenkins.getUpdateCenter();
        List<UpdateSite> sites = updateCenter.getSites();
        assertEquals(2, sites.size());
        UpdateSite siteOne = sites.get(0);
        assertEquals("default", siteOne.getId());
        assertEquals("https://updates.jenkins.io/update-center.json", siteOne.getUrl());
        UpdateSite siteTwo = sites.get(1);
        assertEquals("experimental", siteTwo.getId());
        assertEquals("https://updates.jenkins.io/experimental/update-center.json", siteTwo.getUrl());

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final Configurator c = context.lookupOrFail(UpdateCenter.class);
        final CNode node = c.describe(updateCenter, context);
        assertNotNull(node);
        Mapping site1 = node.asMapping().get("sites").asSequence().get(1).asMapping();
        assertEquals("experimental", site1.getScalarValue("id"));

    }

}
