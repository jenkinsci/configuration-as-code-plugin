package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.Env;
import io.jenkins.plugins.casc.misc.EnvVarsRule;
import io.jenkins.plugins.casc.misc.Envs;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DetectMissingVaultPluginTest {

    @Rule
    public LoggerRule logging = new LoggerRule();

    @Rule
    public EnvVarsRule envVarsRule = new EnvVarsRule();

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test(expected = ConfiguratorException.class)
    @Envs(
        @Env(name = "CASC_VAULT_PW", value = "TEST")
    )
    public void missingVaultPluginShouldThrowException() throws ConfiguratorException {
        assertThat(System.getenv("CASC_VAULT_PW"), is("TEST"));
        ConfigurationAsCode.get().configure(getClass().getResource("admin.yml").toExternalForm());
    }
}
