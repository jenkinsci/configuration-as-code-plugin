package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.branch.OrganizationFolder;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

// import static org.hamcrest.MatcherAssert.assertThat;
// import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
// import static org.junit.Assert.assertTrue;

public class GiteaOrganisationFolderTest {

    @Rule
    public JenkinsRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("SeedJobTest_withGiteaOrganisation.yml")
    public void configure_gitea_organisation_folder_seed_job() {
        final Jenkins jenkins = Jenkins.get();

        OrganizationFolder folder = (OrganizationFolder) jenkins.getItem("Gitea Organization Folder");
        assertNotNull(folder);
    }
}
