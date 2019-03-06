package io.jenkins.plugins.casc.core;

import hudson.model.UpdateSite;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UpdateCenterConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("UpdateCenter.yml")
    public void shouldSetUpdateCenterSites() {
        List<UpdateSite> sites = j.jenkins.getUpdateCenter().getSites();
        assertEquals(2, sites.size());
        UpdateSite siteOne = sites.get(0);
        assertEquals("default", siteOne.getId());
        assertEquals("https://updates.jenkins.io/update-center.json", siteOne.getUrl());
        UpdateSite siteTwo = sites.get(1);
        assertEquals("experimental", siteTwo.getId());
        assertEquals("https://updates.jenkins.io/experimental/update-center.json", siteTwo.getUrl());
    }

}
