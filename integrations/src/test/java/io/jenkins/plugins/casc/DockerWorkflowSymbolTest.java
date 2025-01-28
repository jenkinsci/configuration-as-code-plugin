package io.jenkins.plugins.casc;

import static io.jenkins.plugins.casc.misc.Util.getUnclassifiedRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import io.jenkins.plugins.casc.model.CNode;
import org.jenkinsci.plugins.docker.workflow.declarative.GlobalConfig;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;

@WithJenkinsConfiguredWithCode
class DockerWorkflowSymbolTest {

    @Test
    @Issue("1260")
    @ConfiguredWithCode("DockerWorkflowSymbol.yml")
    void configure_global_definition(JenkinsConfiguredWithCodeRule j) {
        GlobalConfig config = GlobalConfig.get();

        assertNotNull(config);
        assertEquals("label-casc", config.getDockerLabel());
        assertEquals("my.docker.endpoint", config.getRegistry().getUrl());
        assertEquals("credId", config.getRegistry().getCredentialsId());
    }

    @Test
    @Issue("1260")
    @ConfiguredWithCode("DockerWorkflowSymbol.yml")
    void export_global_definition(JenkinsConfiguredWithCodeRule j) throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getUnclassifiedRoot(context).get("pipeline-model-docker");

        String exported = toYamlString(yourAttribute);

        String expected = toStringFromYamlFile(this, "DockerWorkflowSymbolExpected.yml");

        assertThat(exported, is(expected));
    }
}
