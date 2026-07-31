package io.jenkins.plugins.casc.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import hudson.model.FreeStyleProject;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.util.Objects;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;

public class FreestyleItemConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("freestyle-job.yaml")
    public void shouldCreateFreestyleJob() {
        FreeStyleProject job = (FreeStyleProject) Jenkins.get().getItem("my-freestyle-job");

        assertNotNull("Freestyle job should have been created by JCasC", job);
        assertEquals("My Freestyle Job", job.getDisplayName());
        assertEquals("Configured via JCasC", job.getDescription());
    }

    @Test
    public void shouldUpdateExistingFreestyleJob() throws Exception {
        FreeStyleProject existingJob = j.jenkins.createProject(FreeStyleProject.class, "existing-freestyle-job");
        existingJob.setDescription("Old Description");
        existingJob.setDisplayName("Old Display Name");

        ConfigurationAsCode.get()
                .configure(Objects.requireNonNull(getClass().getResource("update-freestyle-job.yaml"))
                        .toExternalForm());

        FreeStyleProject updatedJob = (FreeStyleProject) Jenkins.get().getItem("existing-freestyle-job");
        assertNotNull(updatedJob);
        assertEquals("Updated Freestyle Job", updatedJob.getDisplayName());
        assertEquals("Updated via JCasC", updatedJob.getDescription());
    }
}
