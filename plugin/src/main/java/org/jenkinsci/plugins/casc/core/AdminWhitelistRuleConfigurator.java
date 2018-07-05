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

import com.google.inject.Injector;
import hudson.Extension;
import jenkins.model.Jenkins;
import jenkins.security.s2m.AdminWhitelistRule;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.BaseConfigurator;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Virtual configurator for Remoting security settings.
 * See the unit tests for configuration examples.
 * @author Oleg Nenashev
 */
@Extension
@Restricted(NoExternalUse.class)
public class AdminWhitelistRuleConfigurator extends BaseConfigurator<AdminWhitelistRule> {

    @Override
    public String getName() {
        return "remotingSecurity";
    }

    @Override
    public Class<AdminWhitelistRule> getTarget() {
        return AdminWhitelistRule.class;
    }

    @Override
    protected AdminWhitelistRule instance(Mapping mapping) {
        Injector injector = Jenkins.getInstance().getInjector();
        return injector.getInstance(AdminWhitelistRule.class);
    }

    @Override
    public Set<Attribute> describe() {
        return new HashSet<>(Arrays.asList(
                new Attribute<AdminWhitelistRule, Boolean>("enabled", Boolean.class)
                        .getter(target -> !target.getMasterKillSwitch())
                        .setter((target, value) -> target.setMasterKillSwitch(!(Boolean)value))
        ));
    }

    @CheckForNull
    @Override
    public CNode describe(AdminWhitelistRule instance) {
        return null; // FIXME
    }
}
