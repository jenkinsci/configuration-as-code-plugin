package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.plugins.git.GitSCMSource;
import org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait;
import org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait;
import org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait.TrustPermission;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.github_branch_source.OriginPullRequestDiscoveryTrait;
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries;
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration;
import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class GlobalLibrariesTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("GlobalLibrariesTest.yml")
    public void configure_global_library() throws Exception {
        assertEquals(1, GlobalLibraries.get().getLibraries().size());
        final LibraryConfiguration library = GlobalLibraries.get().getLibraries().get(0);
        assertEquals("awesome-lib", library.getName());
        final SCMSourceRetriever retriever = (SCMSourceRetriever) library.getRetriever();
        final GitSCMSource scm = (GitSCMSource) retriever.getScm();
        assertEquals("https://github.com/jenkins-infra/pipeline-library.git", scm.getRemote());

    }

    @Issue("JENKINS-57557")
    @Test
    @ConfiguredWithCode("GlobalLibrariesGitHubTest.yml")
    public void configure_global_library_using_github() throws Exception {
        assertEquals(1, GlobalLibraries.get().getLibraries().size());
        final LibraryConfiguration library = GlobalLibraries.get().getLibraries().get(0);
        assertEquals("jenkins-pipeline-lib", library.getName());
        final SCMSourceRetriever retriever = (SCMSourceRetriever) library.getRetriever();
        final GitHubSCMSource scm = (GitHubSCMSource) retriever.getScm();
        assertEquals("e43d6600-ba0e-46c5-8eae-3989bf654055", scm.getId());
        assertEquals("jenkins-infra", scm.getRepoOwner());
        assertEquals("pipeline-library", scm.getRepository());
        assertEquals(3, scm.getTraits().size());
        final BranchDiscoveryTrait branchDiscovery = (BranchDiscoveryTrait) scm.getTraits().get(0);
        assertEquals(1, branchDiscovery.getStrategyId());
        final OriginPullRequestDiscoveryTrait prDiscovery = (OriginPullRequestDiscoveryTrait) scm.getTraits().get(1);
        assertEquals(2, prDiscovery.getStrategyId());
        final ForkPullRequestDiscoveryTrait forkDiscovery = (ForkPullRequestDiscoveryTrait) scm.getTraits().get(2);
        assertEquals(3, forkDiscovery.getStrategyId());
        assertThat(forkDiscovery.getTrust(), instanceOf(TrustPermission.class));
    }
}
