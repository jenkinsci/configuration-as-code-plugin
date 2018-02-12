package org.jenkinsci.plugins.casc.core;

import hudson.security.AuthorizationStrategy;
import org.jenkinsci.plugins.casc.misc.CodeConfiguratorRunner;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertSame;

/**
 * @author Kohsuke Kawaguchi
 */
public class UnsecuredAuthorizationStrategyConfiguratorTest {
    public JenkinsRule j = new JenkinsRule();
    public CodeConfiguratorRunner config = new CodeConfiguratorRunner();

    @Rule
    public RuleChain chain = RuleChain.outerRule(j).around(config);

    @Test
    @ConfiguredWithCode("UnsecuredAuthorizationStrategyConfiguratorTest.yml")
    public void unsecured() throws Exception {
        assertSame(AuthorizationStrategy.UNSECURED, j.jenkins.getAuthorizationStrategy());
    }
}
