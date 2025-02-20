package io.jenkins.plugins.casc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import hudson.model.UpdateCenter;
import hudson.model.UpdateSite;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.List;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
class UpdateCenterConfiguratorTest {

    @Test
    @ConfiguredWithCode("UpdateCenter.yml")
    void shouldSetUpdateCenterSites(JenkinsConfiguredWithCodeRule j) throws Exception {
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
