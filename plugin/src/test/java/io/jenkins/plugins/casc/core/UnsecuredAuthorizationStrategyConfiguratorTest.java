package io.jenkins.plugins.casc.core;

import hudson.security.AuthorizationStrategy;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertSame;

/**
 * @author Kohsuke Kawaguchi
 */
public class UnsecuredAuthorizationStrategyConfiguratorTest {
    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("UnsecuredAuthorizationStrategyConfiguratorTest.yml")
    public void unsecured() throws Exception {
        assertSame(AuthorizationStrategy.UNSECURED, j.jenkins.getAuthorizationStrategy());
    }
}
