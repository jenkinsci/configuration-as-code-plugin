package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import org.jenkinsci.plugins.docker.workflow.declarative.GlobalConfig;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import static io.jenkins.plugins.casc.misc.Util.getUnclassifiedRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DockerWorkflowSymbolTest {

    @ClassRule
    @ConfiguredWithCode("DockerWorkflowSymbol.yml")
    public static JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @Issue("1260")
    public void configure_global_definition() throws Exception {
        GlobalConfig config = GlobalConfig.get();

        assertNotNull(config);
        assertEquals("label-casc", config.getDockerLabel());
        assertEquals("my.docker.endpoint", config.getRegistry().getUrl());
        assertEquals("credId", config.getRegistry().getCredentialsId());
    }

    @Test
    @Issue("1260")
    public void export_global_definition() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getUnclassifiedRoot(context).get("pipeline-model-docker");

        String exported = toYamlString(yourAttribute);

        String expected = toStringFromYamlFile(this, "DockerWorkflowSymbolExpected.yml");

        assertThat(exported, is(expected));
    }

}
