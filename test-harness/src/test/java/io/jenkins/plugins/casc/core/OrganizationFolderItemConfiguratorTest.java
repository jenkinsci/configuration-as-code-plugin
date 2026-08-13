package io.jenkins.plugins.casc.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import hudson.model.FreeStyleProject;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.util.Objects;
import jenkins.branch.OrganizationFolder;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMNavigator;
import org.junit.Rule;
import org.junit.Test;

public class OrganizationFolderItemConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("OrganizationFolder.yml")
    public void should_configure_organization_folder() {
        OrganizationFolder folder = (OrganizationFolder) j.jenkins.getItem("my-github-org");

        assertThat("Organization Folder should be created", folder, is(notNullValue()));
        assertEquals("my-github-org", folder.getName());
        assertEquals(
                "Should have exactly one navigator", 1, folder.getNavigators().size());
        assertTrue(
                "Navigator should be of type GitHubSCMNavigator",
                folder.getNavigators().get(0) instanceof GitHubSCMNavigator);

        GitHubSCMNavigator navigator =
                (GitHubSCMNavigator) folder.getNavigators().get(0);
        assertEquals("test-org", navigator.getRepoOwner());
    }

    @Test
    @ConfiguredWithCode("OrganizationFolder.yml")
    public void should_update_existing_organization_folder_on_reload() {
        ConfigurationAsCode.get()
                .configure(Objects.requireNonNull(
                                OrganizationFolderItemConfiguratorTest.class.getResource("OrganizationFolder.yml"))
                        .toExternalForm());

        OrganizationFolder folder = (OrganizationFolder) j.jenkins.getItem("my-github-org");

        assertThat("Organization Folder should still exist", folder, is(notNullValue()));
        assertEquals("my-github-org", folder.getName());
        assertEquals(1, folder.getNavigators().size());
    }

    @Test
    public void should_fail_gracefully_when_item_exists_with_wrong_type() throws Exception {
        j.jenkins.createProject(FreeStyleProject.class, "my-github-org");

        try {
            ConfigurationAsCode.get()
                    .configure(Objects.requireNonNull(
                                    OrganizationFolderItemConfiguratorTest.class.getResource("OrganizationFolder.yml"))
                            .toExternalForm());
            fail("Expected a ConfiguratorException to be thrown");
        } catch (ConfiguratorException e) {
            assertTrue(
                    "Message should contain our custom error",
                    e.getMessage().contains("already exists but is not an OrganizationFolder"));
        }
    }

    @Test
    public void should_describe_name_attribute() {
        assertThat(
                new OrganizationFolderItemConfigurator()
                        .describe().stream()
                                .anyMatch(attribute -> attribute.getName().equals("name")),
                is(true));
    }
}
