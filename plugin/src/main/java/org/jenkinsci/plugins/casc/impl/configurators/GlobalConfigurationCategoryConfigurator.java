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

package org.jenkinsci.plugins.casc.impl.configurators;

import hudson.ExtensionList;
import hudson.model.Descriptor;
import jenkins.model.GlobalConfigurationCategory;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.BaseConfigurator;
import org.jenkinsci.plugins.casc.RootElementConfigurator;
import org.jenkinsci.plugins.casc.impl.configurators.DescriptorConfigurator;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;

import javax.annotation.CheckForNull;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Restricted(Beta.class)
public class GlobalConfigurationCategoryConfigurator extends BaseConfigurator<GlobalConfigurationCategory> implements RootElementConfigurator<GlobalConfigurationCategory> {

    private final GlobalConfigurationCategory category;

    public GlobalConfigurationCategoryConfigurator(GlobalConfigurationCategory category) {
        this.category = category;
    }

    @Override
    public String getName() {
        final Class c = category.getClass();
        final Symbol symbol = (Symbol) c.getAnnotation(Symbol.class);
        if (symbol != null) return symbol.value()[0];

        String name = c.getSimpleName();
        name = StringUtils.remove(name, "Global");
        name = StringUtils.remove(name, "Configuration");
        name = StringUtils.remove(name, "Category");
        return name;
    }

    @Override
    public Class getTarget() {
        return category.getClass();
    }

    @Override
    public GlobalConfigurationCategory getTargetComponent() {
        return category;
    }

    @Override
    protected GlobalConfigurationCategory instance(Mapping mapping) {
        return category;
    }

    @Override
    public Set<Attribute> describe() {

        Set<Attribute> attributes = new HashSet<>();
        final ExtensionList<Descriptor> descriptors = Jenkins.getInstance().getExtensionList(Descriptor.class);
        for (Descriptor descriptor : descriptors) {
            if (descriptor.getCategory() == category && descriptor.getGlobalConfigPage() != null) {

                final DescriptorConfigurator configurator = new DescriptorConfigurator(descriptor);
                attributes.add(new Attribute(configurator.getName(), configurator.getTarget()) {
                    @Override
                    public void setValue(Object target, Object value) throws Exception {
                        // No actual attribute to set, DescriptorRootElementConfigurator manages singleton Descriptor components
                    }
                });
            }
        }
        return attributes;
    }

    @CheckForNull
    @Override
    public CNode describe(GlobalConfigurationCategory instance) {
        return null; // FIXME
    }
}
