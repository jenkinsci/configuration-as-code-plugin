/*
 * Copyright (c) 2018 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.casc.core;

import hudson.EnvVars;
import hudson.model.TaskListener;
import hudson.security.AuthorizationStrategy;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import org.hamcrest.CoreMatchers;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class JenkinsConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("Primitives.yml")
    public void jenkins_primitive_attributes() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();
        assertEquals(6666, jenkins.getSlaveAgentPort());
        assertEquals(false, jenkins.isUsageStatisticsCollected());
    }

    @Test
    @ConfiguredWithCode("HeteroDescribable.yml")
    public void jenkins_abstract_describable_attributes() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();
        assertTrue(jenkins.getSecurityRealm() instanceof HudsonPrivateSecurityRealm);
        assertTrue(jenkins.getAuthorizationStrategy() instanceof FullControlOnceLoggedInAuthorizationStrategy);
        assertFalse(((FullControlOnceLoggedInAuthorizationStrategy) jenkins.getAuthorizationStrategy()).isAllowAnonymousRead());
    }

    @Test
    @Issue("Issue #60")
    public void shouldHaveAuthStrategyConfigurator() throws Exception {
        Configurator c = Configurator.lookup(Jenkins.class);
        Attribute attr = c.getAttribute("authorizationStrategy");
        assertNotNull(attr);
        // Apparently Java always thinks that labmdas are equal
        //assertTrue("The operation should not be NOOP", JenkinsConfigurator.NOOP != attr.getSetter());
        attr.setValue(j.jenkins, new AuthorizationStrategy.Unsecured());

        assertThat("Authorization strategy has not been set",
                j.jenkins.getAuthorizationStrategy(),
                CoreMatchers.instanceOf(AuthorizationStrategy.Unsecured.class));
    }

    @Test
    @Issue("Issue #173")
    @ConfiguredWithCode("SetEnvironmentVariable.yml")
    public void shouldSetEnvironmentVariable() throws Exception {
        final DescribableList<NodeProperty<?>, NodePropertyDescriptor> properties = Jenkins.getInstance().getNodeProperties();
        EnvVars env = new EnvVars();
        for (NodeProperty<?> property : properties) {
            property.buildEnvVars(env, TaskListener.NULL);
        }
        assertEquals("BAR", env.get("FOO"));
    }

}
