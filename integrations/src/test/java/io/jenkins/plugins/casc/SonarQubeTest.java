package io.jenkins.plugins.casc;

import static io.jenkins.plugins.casc.misc.Util.convertYamlFileToJson;
import static io.jenkins.plugins.casc.misc.Util.validateSchema;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import hudson.plugins.sonar.SonarGlobalConfiguration;
import hudson.plugins.sonar.SonarInstallation;
import hudson.plugins.sonar.SonarRunnerInstallation;
import hudson.plugins.sonar.model.TriggersConfig;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import java.util.Objects;
import jenkins.model.GlobalConfiguration;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SonarQubeTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("sonarqube/README.md")
    public void configure_sonar_globalconfig() {

        final SonarGlobalConfiguration configuration = GlobalConfiguration.all().get(SonarGlobalConfiguration.class);
        assertTrue(Objects.requireNonNull(configuration).isBuildWrapperEnabled());
        final SonarInstallation installation = configuration.getInstallations()[0];
        assertEquals("TEST", installation.getName());
        assertEquals("http://url:9000", installation.getServerUrl());
        assertEquals("token", installation.getCredentialsId());
        assertEquals("mojoVersion", installation.getMojoVersion());
        assertEquals("additionalAnalysisProperties", installation.getAdditionalAnalysisProperties());
        final TriggersConfig triggers = installation.getTriggers();
        assertTrue(triggers.isSkipScmCause());
        assertTrue(triggers.isSkipUpstreamCause());
        assertEquals("envVar", triggers.getEnvVar());
    }

    @Test
    @ConfiguredWithReadme("sonarqube/README.md")
    public void configure_sonar_runner_installation() {
        SonarRunnerInstallation.DescriptorImpl descriptor =
                j.jenkins.getDescriptorByType(SonarRunnerInstallation.DescriptorImpl.class);
        SonarRunnerInstallation[] installations = descriptor.getInstallations();

        assertEquals(1, installations.length);
        SonarRunnerInstallation installation = installations[0];
        assertEquals("SonarScanner", installation.getName());
        assertEquals("/opt/sonar-scanner", installation.getHome());
    }

    @Test
    public void validJsonSchema() throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "sonarSchema.yml")), empty());
    }

    @Test
    @Ignore
    public void validFullJsonSchema() throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "sonarSchemaFull.yml")), empty());
    }
}
