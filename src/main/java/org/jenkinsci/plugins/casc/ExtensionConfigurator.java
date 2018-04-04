package org.jenkinsci.plugins.casc;

import hudson.ExtensionList;
import jenkins.model.Jenkins;

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
    public Object configure(Object c) throws ConfiguratorException {
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
                    final Class k = attribute.getType();
                    final Configurator configurator = Configurator.lookup(k);
                    if (configurator == null) throw new IllegalStateException("No configurator implementation to manage "+ k);
                    final Object value = configurator.configure(config.get(name));
                    try {
                        attribute.setValue(o, value);
                    } catch (Exception e) {
                        throw new ConfiguratorException(this, "Failed to set attribute " + attribute, e);
                    }
                }
            }
        }

        return o;
    }

}
