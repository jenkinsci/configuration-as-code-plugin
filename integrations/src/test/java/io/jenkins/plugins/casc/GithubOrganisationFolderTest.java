package io.jenkins.plugins.casc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import hudson.model.TopLevelItem;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.branch.OrganizationFolder;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMNavigator;
import org.junit.Rule;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class GithubOrganisationFolderTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    // @Test
    // Fails as Items do override submit() with manual data-binding implementation
    @ConfiguredWithCode("GithubOrganisationFolderTest.yml")
    public void configure_github_organisation_folder_seed_job() {
        final TopLevelItem job = Jenkins.get().getItem("ndeloof");
        assertNotNull(job);
        assertTrue(job instanceof OrganizationFolder);
        OrganizationFolder folder = (OrganizationFolder) job;
        assertEquals(1, folder.getNavigators().size());
        final GitHubSCMNavigator github = folder.getNavigators().get(GitHubSCMNavigator.class);
        assertNotNull(github);
        assertEquals("ndeloof", github.getRepoOwner());
    }
}
