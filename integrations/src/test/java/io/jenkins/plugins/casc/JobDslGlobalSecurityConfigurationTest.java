package io.jenkins.plugins.casc;

import javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration;
import jenkins.model.GlobalConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Created by odavid on 23/12/2017.
 */
public class JobDslGlobalSecurityConfigurationTest {

    @Rule
    public RestartableJenkinsRule j = new RestartableJenkinsRule();

    @Test
    public void test_global_dsl_security_can_be_applied() {
        j.addStep(validateGlobalDSLSecurity);
    }

    @Test
    @Issue("#253")
    public void test_global_dsl_security_can_be_reapplied_after_restart() {
        j.addStep(validateGlobalDSLSecurity);
        j.addStep(validateGlobalDSLSecurityAfterRestart, true);
    }

    private GlobalJobDslSecurityConfiguration getGlobalJobDslSecurityConfiguration() {
        final GlobalJobDslSecurityConfiguration dslSecurity = GlobalConfiguration.all()
            .get(GlobalJobDslSecurityConfiguration.class);
        assertNotNull(dslSecurity);
        return dslSecurity;
    }

    private void configure() throws ConfiguratorException {
        ConfigurationAsCode.get().configure(getClass().getResource("JobDslGlobalSecurityConfigurationTest.yml").toExternalForm());
    }

    private Statement validateGlobalDSLSecurity = new Statement() {

        @Override
        public void evaluate() throws Throwable {
            final GlobalJobDslSecurityConfiguration dslSecurity = getGlobalJobDslSecurityConfiguration();

            dslSecurity.setUseScriptSecurity(true);
            assertThat("ScriptSecurity", dslSecurity.isUseScriptSecurity(), is(true));

            configure();

            assertThat("ScriptSecurity", dslSecurity.isUseScriptSecurity(), is(false));
        }
    };

    private Statement validateGlobalDSLSecurityAfterRestart = new Statement() {

        @Override
        public void evaluate() throws Throwable {
            final GlobalJobDslSecurityConfiguration dslSecurity = getGlobalJobDslSecurityConfiguration();

            // step 1 configuration still applies
            assertThat("ScriptSecurity", dslSecurity.isUseScriptSecurity(), is(false));

            configure();

            assertThat("ScriptSecurity", dslSecurity.isUseScriptSecurity(), is(false));
        }
    };

}
