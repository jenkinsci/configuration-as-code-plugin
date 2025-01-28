package io.jenkins.plugins.casc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration;
import jenkins.model.GlobalConfiguration;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Created by odavid on 23/12/2017.
 */
@WithJenkins
class JobDslGlobalSecurityConfigurationTest {

    @Test
    void test_global_dsl_security_can_be_applied(@SuppressWarnings("unused") JenkinsRule j) {
        GlobalJobDslSecurityConfiguration dslSecurity = getGlobalJobDslSecurityConfiguration();
        dslSecurity.setUseScriptSecurity(true);

        assertThat("ScriptSecurity", dslSecurity.isUseScriptSecurity(), is(true));
        configure();
        assertThat("ScriptSecurity", dslSecurity.isUseScriptSecurity(), is(false));
    }

    @Test
    @Issue("#253")
    void test_global_dsl_security_can_be_reapplied_after_restart(JenkinsRule j) throws Throwable {
        GlobalJobDslSecurityConfiguration dslSecurity = getGlobalJobDslSecurityConfiguration();
        dslSecurity.setUseScriptSecurity(true);

        assertThat("ScriptSecurity", dslSecurity.isUseScriptSecurity(), is(true));
        configure();
        assertThat("ScriptSecurity", dslSecurity.isUseScriptSecurity(), is(false));

        j.restart();

        dslSecurity = getGlobalJobDslSecurityConfiguration();

        assertThat("ScriptSecurity", dslSecurity.isUseScriptSecurity(), is(false));
        configure();
        assertThat("ScriptSecurity", dslSecurity.isUseScriptSecurity(), is(false));
    }

    private GlobalJobDslSecurityConfiguration getGlobalJobDslSecurityConfiguration() {
        GlobalJobDslSecurityConfiguration dslSecurity =
                GlobalConfiguration.all().get(GlobalJobDslSecurityConfiguration.class);
        assertNotNull(dslSecurity);
        return dslSecurity;
    }

    private void configure() throws ConfiguratorException {
        ConfigurationAsCode.get()
                .configure(getClass()
                        .getResource("JobDslGlobalSecurityConfigurationTest.yml")
                        .toExternalForm());
    }
}
