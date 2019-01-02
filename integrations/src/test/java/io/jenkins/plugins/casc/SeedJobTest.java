package io.jenkins.plugins.casc;

import static org.junit.Assert.assertNotNull;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.EnvVarsRule;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SeedJobTest {

    @Rule
    public RuleChain rc = RuleChain.outerRule(new EnvVarsRule()
            .env("SEED_JOB_PATH", "./src/test/resources/io/jenkins/plugins/casc/testJob2.groovy"))
            .around(new JenkinsConfiguredWithCodeRule());

    @Test
    @ConfiguredWithCode("SeedJobTest.yml")
    public void configure_seed_job() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();
        assertNotNull(jenkins.getItem("testJob1"));
        assertNotNull(jenkins.getItem("testJob2"));
    }

    @Test
    @ConfiguredWithCode("SeedJobTest_withSecrets.yml")
    public void configure_seed_job_with_secrets() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();
        assertNotNull(jenkins.getItem("testJob2"));
    }
}
