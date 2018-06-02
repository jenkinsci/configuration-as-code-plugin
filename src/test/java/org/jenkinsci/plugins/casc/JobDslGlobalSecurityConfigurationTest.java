package org.jenkinsci.plugins.casc;

import javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration;
import jenkins.model.GlobalConfiguration;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by odavid on 23/12/2017.
 */
public class JobDslGlobalSecurityConfigurationTest {

    @Rule
    public RestartableJenkinsRule restartableJenkins = new RestartableJenkinsRule();

    @Ignore
    @Test
    public void global_dsl_security() throws Exception {
        final GlobalJobDslSecurityConfiguration dslSecurity = GlobalConfiguration.all()
                .get(GlobalJobDslSecurityConfiguration.class);

        dslSecurity.setUseScriptSecurity(true);
        assertThat("ScriptSecurity", dslSecurity.isUseScriptSecurity(), is(true));

        ConfigurationAsCode.get().configure(getClass().getResource("JobDslGlobalSecurityConfigurationTest.yml").toExternalForm());

        assertThat("ScriptSecurity", dslSecurity.isUseScriptSecurity(), is(false));
    }

    @Test
    public void global_dsl_security_can_be_reapplied_after_restart() {
        restartableJenkins.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final GlobalJobDslSecurityConfiguration dslSecurity = GlobalConfiguration.all()
                    .get(GlobalJobDslSecurityConfiguration.class);

                dslSecurity.setUseScriptSecurity(true);
                assertThat("ScriptSecurity", dslSecurity.isUseScriptSecurity(), is(true));

                ConfigurationAsCode.get().configure(getClass().getResource("JobDslGlobalSecurityConfigurationTest.yml").toExternalForm());

                assertThat("ScriptSecurity", dslSecurity.isUseScriptSecurity(), is(false));
            }
        });

        restartableJenkins.addStep(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final GlobalJobDslSecurityConfiguration dslSecurity = GlobalConfiguration.all()
                    .get(GlobalJobDslSecurityConfiguration.class);

                // step 1 configuration still applies
                assertThat("ScriptSecurity", dslSecurity.isUseScriptSecurity(), is(false));

                // this breaks
                ConfigurationAsCode.get().configure(getClass().getResource("JobDslGlobalSecurityConfigurationTest.yml").toExternalForm());

                assertThat("ScriptSecurity", dslSecurity.isUseScriptSecurity(), is(false));
            }
        }, true);
    }

}
