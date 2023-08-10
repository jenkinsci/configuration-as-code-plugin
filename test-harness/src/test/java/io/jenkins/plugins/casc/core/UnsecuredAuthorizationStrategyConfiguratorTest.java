package io.jenkins.plugins.casc.core;

import static org.junit.Assert.assertSame;

import hudson.security.AuthorizationStrategy;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Kohsuke Kawaguchi
 */
public class UnsecuredAuthorizationStrategyConfiguratorTest {
    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("UnsecuredAuthorizationStrategyConfiguratorTest.yml")
    public void unsecured() {
        assertSame(AuthorizationStrategy.UNSECURED, j.jenkins.getAuthorizationStrategy());
    }
}
