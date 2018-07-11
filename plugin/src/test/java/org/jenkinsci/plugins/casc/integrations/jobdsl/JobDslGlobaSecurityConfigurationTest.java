package org.jenkinsci.plugins.casc.integrations.jobdsl;

import javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration;
import jenkins.model.GlobalConfiguration;
import org.jenkinsci.plugins.casc.ConfigurationAsCode;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by odavid on 23/12/2017.
 */
public class JobDslGlobaSecurityConfigurationTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void global_dsl_security() throws Exception {
        final GlobalJobDslSecurityConfiguration dslSecurity = GlobalConfiguration.all()
                .get(GlobalJobDslSecurityConfiguration.class);

        dslSecurity.setUseScriptSecurity(true);
        assertThat("ScriptSecurity", dslSecurity.isUseScriptSecurity(), is(true));

        ConfigurationAsCode.get().configure(getClass().getResource("JobDslGlobaSecurityConfigurationTest.yml").toExternalForm());

        assertThat("ScriptSecurity", dslSecurity.isUseScriptSecurity(), is(false));
    }

}
