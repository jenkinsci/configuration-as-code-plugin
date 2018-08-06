package io.jenkins.plugins.casc;

import hudson.plugins.sonar.SonarGlobalConfiguration;
import hudson.plugins.sonar.SonarInstallation;
import hudson.plugins.sonar.model.TriggersConfig;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.GlobalConfiguration;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SonarQubeTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("SonarQubeTest.yml")
    public void configure_sonar_globalconfig() throws Exception {

        final SonarGlobalConfiguration configuration = GlobalConfiguration.all().get(SonarGlobalConfiguration.class);
        assertTrue(configuration.isBuildWrapperEnabled());
        final SonarInstallation installation = configuration.getInstallations()[0];
        assertEquals("TEST", installation.getName());
        assertEquals("http://url:9000", installation.getServerUrl());
        assertEquals("token", installation.getServerAuthenticationToken());
        assertEquals("mojoVersion", installation.getMojoVersion());
        assertEquals("additionalAnalysisProperties", installation.getAdditionalAnalysisProperties());
        final TriggersConfig triggers = installation.getTriggers();
        assertTrue(triggers.isSkipScmCause());
        assertTrue(triggers.isSkipUpstreamCause());
        assertEquals("envVar", triggers.getEnvVar());
    }

}
