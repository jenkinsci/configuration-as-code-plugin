package org.jenkinsci.plugins.casc.core;

import hudson.security.AuthorizationStrategy;
import org.jenkinsci.plugins.casc.misc.TestConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class UnsecuredAuthorizationStrategyConfiguratorTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void unsecured() throws Exception {
        new TestConfiguration("UnsecuredAuthorizationStrategyConfiguratorTest.yml").configure(getClass());
        assertSame(AuthorizationStrategy.UNSECURED, j.jenkins.getAuthorizationStrategy());
    }
}
