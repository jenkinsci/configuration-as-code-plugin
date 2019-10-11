package io.jenkins.plugins.casc;

import hudson.plugins.tfs.TeamCollectionConfiguration;
import hudson.plugins.tfs.TeamPluginGlobalConfig;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.model.GlobalConfiguration;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author v1v (Victor Martinez)
 */
public class TfsTest {

    @ClassRule
    @ConfiguredWithReadme("tfs/README.md")
    public static JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    public void configure_tfs() {
        final TeamPluginGlobalConfig configuration = GlobalConfiguration.all().get(TeamPluginGlobalConfig.class);
        assertNotNull(configuration);
        assertThat(configuration.getCollectionConfigurations(), hasSize(1));
        TeamCollectionConfiguration team = configuration.getCollectionConfigurations().get(0);
        assertThat(team.getCollectionUrl(), is("http://test.com"));
        assertThat(team.getCredentialsId(), is("tfsCredentials"));
        assertTrue(configuration.isEnableTeamPushTriggerForAllJobs());
        assertTrue(configuration.isEnableTeamStatusForAllJobs());
        assertTrue(configuration.isConfigFolderPerNode());
    }
}
