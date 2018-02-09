package org.jenkinsci.plugins.casc;

import javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration;
import jenkins.model.GlobalConfiguration;
import org.jenkinsci.plugins.casc.misc.TestConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by odavid on 23/12/2017.
 */
public class JobDslGlobaSecurityConfigurationTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void global_dsl_security() throws Exception {
        final GlobalJobDslSecurityConfiguration dslSecurity =
                GlobalConfiguration.all().get(GlobalJobDslSecurityConfiguration.class);

        dslSecurity.setUseScriptSecurity(true);
        assertTrue(dslSecurity.isUseScriptSecurity());

        new TestConfiguration("JobDslGlobaSecurityConfigurationTest.yml").configure(getClass());

        assertFalse(dslSecurity.isUseScriptSecurity());
    }

}
