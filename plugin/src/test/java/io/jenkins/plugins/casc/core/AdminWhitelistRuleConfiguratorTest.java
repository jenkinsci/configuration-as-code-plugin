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
package io.jenkins.plugins.casc.core;

import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import jenkins.model.Jenkins;
import jenkins.security.s2m.AdminWhitelistRule;
import jenkins.security.s2m.MasterKillSwitchConfiguration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.For;
import org.jvnet.hudson.test.Issue;

import static org.junit.Assert.assertEquals;

@For(AdminWhitelistRule.class)
public class AdminWhitelistRuleConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule config = new JenkinsConfiguredWithCodeRule();

    @Test
    @Issue("Issue #28")
    @ConfiguredWithCode("AdminWhitelistRuleConfigurator/Agent2MasterSecurityKillSwitch_enabled.yml")
    public void checkM2ASecurityKillSwitch_disabled() {
        final Jenkins jenkins = Jenkins.getInstance();
        AdminWhitelistRule rule = jenkins.getInjector().getInstance(AdminWhitelistRule.class);
        Assert.assertFalse("MasterToAgent Security should be disabled", rule.getMasterKillSwitch());
    }

    @Test
    @Issue("Issue #28")
    @ConfiguredWithCode("AdminWhitelistRuleConfigurator/Agent2MasterSecurityKillSwitch_disabled.yml")
    public void checkM2ASecurityKillSwitch_enabled() {
        final Jenkins jenkins = Jenkins.getInstance();
        AdminWhitelistRule rule = jenkins.getInjector().getInstance(AdminWhitelistRule.class);
        Assert.assertTrue("MasterToAgent Security should be enabled", rule.getMasterKillSwitch());
    }

    @Test
    @Issue("Issue #172")
    @ConfiguredWithCode("AdminWhitelistRuleConfigurator/Agent2MasterSecurityKillSwitch_enabled.yml")
    public void checkA2MAccessControl_enabled() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();
        MasterKillSwitchConfiguration config = jenkins.getDescriptorByType(MasterKillSwitchConfiguration.class);
        Assert.assertTrue("Agent → Master Access Control should be enabled", config.getMasterToSlaveAccessControl());
        AdminWhitelistRule rule = jenkins.getInjector().getInstance(AdminWhitelistRule.class);
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final Configurator c = context.lookupOrFail(AdminWhitelistRule.class);
        final CNode node = c.describe(rule, context);
        final Mapping agent = node.asMapping();
        assertEquals("true", agent.get("enabled").toString());
    }

    @Test
    @Issue("Issue #172")
    @ConfiguredWithCode("AdminWhitelistRuleConfigurator/Agent2MasterSecurityKillSwitch_disabled.yml")
    public void checkA2MAccessControl_disable() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();
        MasterKillSwitchConfiguration config = jenkins.getDescriptorByType(MasterKillSwitchConfiguration.class);
        Assert.assertFalse("Agent → Master Access Control should be disabled", config.getMasterToSlaveAccessControl());
        AdminWhitelistRule rule = jenkins.getInjector().getInstance(AdminWhitelistRule.class);
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final Configurator c = context.lookupOrFail(AdminWhitelistRule.class);
        final CNode node = c.describe(rule, context);
        final Mapping agent = node.asMapping();
        assertEquals("false", agent.get("enabled").toString());
    }

    
}
