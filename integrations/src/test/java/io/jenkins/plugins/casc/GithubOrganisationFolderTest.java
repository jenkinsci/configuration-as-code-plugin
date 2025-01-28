package io.jenkins.plugins.casc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import hudson.model.TopLevelItem;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import jenkins.branch.OrganizationFolder;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMNavigator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@WithJenkinsConfiguredWithCode
class GithubOrganisationFolderTest {

    @Test
    @Disabled("Fails as Items do override submit() with manual data-binding implementation")
    @ConfiguredWithCode("GithubOrganisationFolderTest.yml")
    void configure_github_organisation_folder_seed_job(JenkinsConfiguredWithCodeRule j) {
        final TopLevelItem job = Jenkins.get().getItem("ndeloof");
        assertNotNull(job);
        assertInstanceOf(OrganizationFolder.class, job);
        OrganizationFolder folder = (OrganizationFolder) job;
        assertEquals(1, folder.getNavigators().size());
        final GitHubSCMNavigator github = folder.getNavigators().get(GitHubSCMNavigator.class);
        assertNotNull(github);
        assertEquals("ndeloof", github.getRepoOwner());
    }
}
