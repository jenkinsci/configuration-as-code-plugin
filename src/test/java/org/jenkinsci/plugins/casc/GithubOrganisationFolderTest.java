package org.jenkinsci.plugins.casc;

import com.nirima.jenkins.plugins.docker.DockerCloud;
import com.nirima.jenkins.plugins.docker.DockerTemplate;
import hudson.model.TopLevelItem;
import jenkins.branch.OrganizationFolder;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMNavigator;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class GithubOrganisationFolderTest {


    @Rule
    public JenkinsRule j = new JenkinsRule();

    // @Test
    // Fails as Items do override submit() with manual data-binding implementation
    public void configure_github_organisation_folder_seed_job() throws Exception {
        ConfigurationAsCode.configure(getClass().getResourceAsStream("GithubOrganisationFolderTest.yml"));

        final TopLevelItem job = Jenkins.getInstance().getItem("ndeloof");
        assertNotNull(job);
        assertTrue(job instanceof OrganizationFolder);
        OrganizationFolder folder = (OrganizationFolder) job;
        assertEquals(1, folder.getNavigators().size());
        final GitHubSCMNavigator github = folder.getNavigators().get(GitHubSCMNavigator.class);
        assertNotNull(github);
        assertEquals("ndeloof", github.getRepoOwner());
    }
}
