package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.model.GlobalConfiguration;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertTrue;

/**
 * @author v1v (Victor Martinez)
 */
public class GitHubTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("github/README.md")
    public void configure_github() {

        final GitHubPluginConfig configuration = GlobalConfiguration.all().get(GitHubPluginConfig.class);
        assertThat(configuration.getConfigs(), hasSize(1));

        GitHubServerConfig config = configuration.getConfigs().get(0);
        assertThat(config.getApiUrl(), is("https://github.domain.local/api/v3"));
        assertThat(config.getCredentialsId(), is("[GitHubEEUser]"));
        assertThat(config.getName(), is("InHouse GitHub EE"));
        assertTrue(config.isManageHooks());

    }
}
