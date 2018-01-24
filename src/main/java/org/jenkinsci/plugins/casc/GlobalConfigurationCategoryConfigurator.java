package org.jenkinsci.plugins.casc;

import hudson.ExtensionList;
import hudson.model.Descriptor;
import jenkins.model.GlobalConfigurationCategory;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class GlobalConfigurationCategoryConfigurator extends BaseConfigurator implements RootElementConfigurator {

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
    public Object configure(Object config) throws Exception {
        configure((Map) config, category);
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

}
