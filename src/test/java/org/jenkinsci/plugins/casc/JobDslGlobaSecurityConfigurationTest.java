package org.jenkinsci.plugins.casc;

import javaposse.jobdsl.plugin.GlobalJobDslSecurityConfiguration;
import jenkins.model.GlobalConfiguration;
import org.jenkinsci.plugins.casc.misc.CodeConfiguratorRunner;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by odavid on 23/12/2017.
 */
public class JobDslGlobaSecurityConfigurationTest {

    public JenkinsRule j = new JenkinsRule();
    public CodeConfiguratorRunner config = new CodeConfiguratorRunner();

    @Rule
    public RuleChain chain = RuleChain.outerRule(j)
            .around(new ExternalResource() {
                @Override
                protected void before() throws Throwable {
                    final GlobalJobDslSecurityConfiguration dslSecurity =
                            GlobalConfiguration.all().get(GlobalJobDslSecurityConfiguration.class);

                    dslSecurity.setUseScriptSecurity(true);
                    assertTrue(dslSecurity.isUseScriptSecurity());
                }
            })
            .around(config);

    @Test
    @ConfiguredWithCode("JobDslGlobaSecurityConfigurationTest.yml")
    public void global_dsl_security() throws Exception {
        final GlobalJobDslSecurityConfiguration dslSecurity =
                GlobalConfiguration.all().get(GlobalJobDslSecurityConfiguration.class);

        assertFalse(dslSecurity.isUseScriptSecurity());
    }

}
