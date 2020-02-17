package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import org.jenkinsci.plugins.pipeline.modeldefinition.config.GlobalConfig;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PipelineModelDefinitionTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("pipeline-model-definition/README.md")
    public void configure_global_definition() throws Exception {
        GlobalConfig config = GlobalConfig.get();

        assertNotNull(config);
        assertEquals("label-casc", config.getDockerLabel());
        assertEquals("my.docker.endpoint", config.getRegistry().getUrl());
        assertEquals("credId", config.getRegistry().getCredentialsId());
    }
}
