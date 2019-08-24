package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.Env;
import io.jenkins.plugins.casc.misc.EnvVarsRule;
import io.jenkins.plugins.casc.misc.Envs;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;

import static io.jenkins.plugins.casc.misc.Util.assertLogContains;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DetectMissingVaultPluginTest {

    public LoggerRule loggerRule = new LoggerRule();

    @Rule
    public RuleChain chain = RuleChain
        .outerRule(loggerRule.record(Logger.getLogger(ConfigurationAsCode.class.getName()), Level.SEVERE).capture(2048))
        .around(new EnvVarsRule())
        .around(new JenkinsRule());

    @Test
    @Envs(
        @Env(name = "CASC_VAULT_PW", value = "TEST")
    )
    public void missingVaultPluginShouldThrowException() throws ConfiguratorException {
        assertThat(System.getenv("CASC_VAULT_PW"), is("TEST"));
        ConfigurationAsCode.get().configure(getClass().getResource("admin.yml").toExternalForm());
        assertLogContains(loggerRule, "Vault secret resolver is not installed, consider installing hashicorp-vault-plugin v2.4.0 or higher");
    }
}
