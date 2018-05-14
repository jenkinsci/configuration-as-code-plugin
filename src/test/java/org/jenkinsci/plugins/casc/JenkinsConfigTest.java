package org.jenkinsci.plugins.casc;

import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.misc.EnvVarsRule;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class JenkinsConfigTest {

    @Rule
    public RuleChain chain = RuleChain.outerRule( new EnvVarsRule()
            .env("CASC_JENKINS_CONFIG", getClass().getResource("JenkinsConfigTest.yml").toExternalForm()))
            .around(new JenkinsConfiguredWithCodeRule());


    @Test
    public void loadFromCASC_JENKINS_CONFIG() {
        assertEquals("configuration as code - JenkinsConfigTest", Jenkins.getInstance().getSystemMessage());
    }
}
