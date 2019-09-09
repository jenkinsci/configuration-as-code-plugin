package io.jenkins.plugins.casc;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.Env;
import io.jenkins.plugins.casc.misc.EnvVarsRule;
import io.jenkins.plugins.casc.misc.Envs;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SeedJobTest {

    private JenkinsRule j;

    @Rule
    public RuleChain rc = RuleChain.outerRule(new EnvVarsRule()
            .set("SEED_JOB_PATH", "./src/test/resources/io/jenkins/plugins/casc/testJob2.groovy")
            .set("REPO_URL", "git://github.com/jenkinsci/configuration-as-code-plugin.git"))
            .around(j = new JenkinsConfiguredWithCodeRule());

    @Test
    @ConfiguredWithCode("SeedJobTest.yml")
    public void configure_seed_job() throws Exception {
        final Jenkins jenkins = Jenkins.get();
        assertNotNull(jenkins.getItem("testJob1"));
        assertNotNull(jenkins.getItem("testJob2"));
    }

    @Test
    @ConfiguredWithCode("SeedJobTest_withSecrets.yml")
    public void configure_seed_job_with_secrets() throws Exception {
        final Jenkins jenkins = Jenkins.get();
        assertNotNull(jenkins.getItem("testJob2"));
    }

    @Test
    @ConfiguredWithCode("SeedJobTest_withEnvVars.yml")
    public void configure_seed_job_with_env_vars() throws Exception {
        final Jenkins jenkins = Jenkins.get();
        assertNotNull(jenkins.getItem("seedJobWithEnvVars"));
    }

    @Test
    @ConfiguredWithCode("SeedJobTest_withSecurityConfig.yml")
    @Envs(
        @Env(name = "SEED_JOB_FOLDER_FILE_PATH", value = ".")
    )
    public void configure_seed_job_with_security_config() throws Exception {
        final Jenkins jenkins = Jenkins.get();

        final GlobalJobDslSecurityConfiguration dslSecurity = GlobalConfiguration.all()
            .get(GlobalJobDslSecurityConfiguration.class);
        assertNotNull(dslSecurity);
        assertThat("ScriptSecurity", dslSecurity.isUseScriptSecurity(), is(false));

        FreeStyleProject seedJobWithSecurityConfig = (FreeStyleProject) jenkins.getItem("seedJobWithSecurityConfig");
        assertNotNull(seedJobWithSecurityConfig);

        assertTrue(seedJobWithSecurityConfig.isInQueue());
        FreeStyleBuild freeStyleBuild = j.buildAndAssertSuccess(seedJobWithSecurityConfig);
        j.assertLogContains("Processing DSL script testJob2.groovy", freeStyleBuild);
        j.assertLogContains("Added items:", freeStyleBuild);
        j.assertLogContains("GeneratedJob{name='testJob2'}", freeStyleBuild);
    }
}
