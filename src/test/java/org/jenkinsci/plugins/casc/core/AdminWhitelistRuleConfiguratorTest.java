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

import jenkins.model.Jenkins;
import jenkins.security.s2m.AdminWhitelistRule;
import org.jenkinsci.plugins.casc.misc.CodeConfiguratorRunner;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.For;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

@For(AdminWhitelistRule.class)
public class AdminWhitelistRuleConfiguratorTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Rule
    public CodeConfiguratorRunner config = new CodeConfiguratorRunner();

    @Test
    @Issue("Issue #28")
    @ConfiguredWithCode("AdminWhitelistRuleConfigurator/Slave2MasterSecurityKillSwitch_enabled.yml")
    public void checkM2SSecurityKillSwitch_enabled() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();
        AdminWhitelistRule rule = jenkins.getInjector().getInstance(AdminWhitelistRule.class);
        Assert.assertTrue("MasterToSlave Security should be enabled", rule.getMasterKillSwitch());
    }

    @Test
    @Issue("Issue #28")
    @ConfiguredWithCode("AdminWhitelistRuleConfigurator/Slave2MasterSecurityKillSwitch_disabled.yml")
    public void checkM2SSecurityKillSwitch_disabled() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();
        AdminWhitelistRule rule = jenkins.getInjector().getInstance(AdminWhitelistRule.class);
        Assert.assertFalse("MasterToSlave Security should be disabled", rule.getMasterKillSwitch());
    }
}
