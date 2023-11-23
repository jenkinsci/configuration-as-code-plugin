package io.jenkins.plugins.casc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import org.jenkinsci.plugins.docker.workflow.declarative.GlobalConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class DockerWorkflowTest {
    @Before
    public void shouldThisRun() {
        assumeTrue(ShouldRun.thisTest());
    }

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("docker-workflow/README.md")
    public void configure_global_definition() {
        GlobalConfig config = GlobalConfig.get();

        assertNotNull(config);
        assertEquals("label-casc", config.getDockerLabel());
        assertEquals("my.docker.endpoint", config.getRegistry().getUrl());
        assertEquals("credId", config.getRegistry().getCredentialsId());
    }
}
