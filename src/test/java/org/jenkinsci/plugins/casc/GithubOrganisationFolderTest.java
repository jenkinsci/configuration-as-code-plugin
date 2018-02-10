package org.jenkinsci.plugins.casc;

import hudson.model.TopLevelItem;
import jenkins.branch.OrganizationFolder;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.misc.CodeConfiguratorRunner;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMNavigator;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class GithubOrganisationFolderTest {

    public JenkinsRule j = new JenkinsRule();
    public CodeConfiguratorRunner config = new CodeConfiguratorRunner();

    @Rule
    public RuleChain chain = RuleChain.outerRule(j).around(config);

    // @Test
    // Fails as Items do override submit() with manual data-binding implementation
    @ConfiguredWithCode("GithubOrganisationFolderTest.yml")
    public void configure_github_organisation_folder_seed_job() throws Exception {
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
