package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.util.List;
import jenkins.branch.OrganizationFolder;
import jenkins.scm.api.trait.SCMTrait;
import org.jenkinsci.plugin.gitea.BranchDiscoveryTrait;
import org.jenkinsci.plugin.gitea.ExcludeArchivedRepositoriesTrait;
import org.jenkinsci.plugin.gitea.ForkPullRequestDiscoveryTrait;
import org.jenkinsci.plugin.gitea.GiteaSCMNavigator;
import org.jenkinsci.plugin.gitea.OriginPullRequestDiscoveryTrait;
import org.jenkinsci.plugin.gitea.SSHCheckoutTrait;
import org.jenkinsci.plugin.gitea.TagDiscoveryTrait;
import org.jenkinsci.plugin.gitea.WebhookRegistrationTrait;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.recipes.WithPlugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GiteaOrganisationFolderTest {

    @Rule
    public JenkinsConfiguredWithCodeRule r = new JenkinsConfiguredWithCodeRule();

    @Test
    @WithPlugin({"branch-api.jpi", "cloudbees-folder.jpi", "scm-api.jpi", "structs.jpi"})
    @ConfiguredWithCode("SeedJobTest_withGiteaOrganisation.yml")
    public void configure_gitea_organisation_folder_seed_job() {
        OrganizationFolder folder = (OrganizationFolder) r.jenkins.getItem("Gitea Organization Folder");
        assertNotNull(folder);

        GiteaSCMNavigator organization = (GiteaSCMNavigator) folder.getNavigators().get(0);
        assertNotNull(organization);

        assertEquals("gitea-token", organization.getCredentialsId());
        assertEquals("https://git.example.com", organization.getServerUrl());
        assertEquals("OWN", organization.getRepoOwner());

        List<SCMTrait<?>> traits = organization.getTraits();
        assertEquals(7, traits.size());

        assertEquals(ExcludeArchivedRepositoriesTrait.class, traits.get(0).getClass());
        assertEquals(TagDiscoveryTrait.class, traits.get(1).getClass());

        SCMTrait<?> trait2 = traits.get(2);
        assertEquals(BranchDiscoveryTrait.class, trait2.getClass());
        assertEquals(1, ((BranchDiscoveryTrait)trait2).getStrategyId());

        SCMTrait<?> trait3 = traits.get(3);
        assertEquals(OriginPullRequestDiscoveryTrait.class, trait3.getClass());
        assertEquals(2, ((OriginPullRequestDiscoveryTrait)trait3).getStrategyId());

        SCMTrait<?> trait4 = traits.get(4);
        assertEquals(ForkPullRequestDiscoveryTrait.class, trait4.getClass());
        assertEquals(1, ((ForkPullRequestDiscoveryTrait)trait4).getStrategyId());
        assertEquals(ForkPullRequestDiscoveryTrait.TrustContributors.class, ((ForkPullRequestDiscoveryTrait)trait4).getTrust().getClass());

        SCMTrait<?> trait5 = traits.get(5);
        assertEquals(WebhookRegistrationTrait.class, trait5.getClass());
        assertEquals("ITEM", ((WebhookRegistrationTrait)trait5).getMode().name());

        SCMTrait<?> trait6 = traits.get(6);
        assertEquals(SSHCheckoutTrait.class, trait6.getClass());
        assertEquals("ssh-gitea", ((SSHCheckoutTrait)trait6).getCredentialsId());
    }
}
