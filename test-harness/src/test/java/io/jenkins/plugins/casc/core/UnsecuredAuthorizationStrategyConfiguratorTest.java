package io.jenkins.plugins.casc.core;

import static org.junit.jupiter.api.Assertions.assertSame;

import hudson.security.AuthorizationStrategy;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import org.junit.jupiter.api.Test;

/**
 * @author Kohsuke Kawaguchi
 */
@WithJenkinsConfiguredWithCode
class UnsecuredAuthorizationStrategyConfiguratorTest {

    @Test
    @ConfiguredWithCode("UnsecuredAuthorizationStrategyConfiguratorTest.yml")
    void unsecured(JenkinsConfiguredWithCodeRule j) {
        assertSame(AuthorizationStrategy.UNSECURED, j.jenkins.getAuthorizationStrategy());
    }
}
