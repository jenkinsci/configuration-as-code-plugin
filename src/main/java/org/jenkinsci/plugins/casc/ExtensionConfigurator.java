package org.jenkinsci.plugins.casc;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A generic {@link Configurator} for {@link hudson.Extension} singletons
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ExtensionConfigurator extends BaseConfigurator {

    private final Class target;

    public ExtensionConfigurator(Class clazz) {
        this.target = clazz;
    }


    @Override
    public Class getTarget() {
        return target;
    }


    @Override
    public Object configure(Object c) throws Exception {
        final ExtensionList list = Jenkins.getInstance().getExtensionList(target);
        if (list.size() != 1) {
            throw new IllegalStateException();
        }
        final Object o = list.get(0);

        if (c instanceof Map) {
            Map config = (Map) c;
            final Set<Attribute> attributes = describe();
            for (Attribute attribute : attributes) {
                final String name = attribute.getName();
                if (config.containsKey(name)) {
                    final Object value = Configurator.lookup(attribute.getType()).configure(config.get(name));
                    attribute.setValue(o, value);
                }
            }
        }

        return o;
    }

}
