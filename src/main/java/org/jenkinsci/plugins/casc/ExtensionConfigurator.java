package org.jenkinsci.plugins.casc;

import hudson.ExtensionList;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;

import javax.annotation.CheckForNull;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A generic {@link Configurator} for {@link hudson.Extension} singletons
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ExtensionConfigurator<T> extends BaseConfigurator<T> {

    private final static Logger logger = Logger.getLogger(ExtensionConfigurator.class.getName());

    private final Class<T> target;

    public ExtensionConfigurator(Class<T> clazz) {
        this.target = clazz;
    }


    @Override
    public Class<T> getTarget() {
        return target;
    }


    @Override
    public T configure(CNode c) throws ConfiguratorException {
        final ExtensionList<T> list = Jenkins.getInstance().getExtensionList(target);
        if (list.size() != 1) {
            throw new IllegalStateException();
        }
        final T o = list.get(0);

        if (c instanceof Map) {
            Mapping config = c.asMapping();
            final Set<Attribute> attributes = describe();
            for (Attribute attribute : attributes) {
                final String name = attribute.getName();
                if (config.containsKey(name)) {
                    final Class k = attribute.getType();
                    final Configurator configurator = Configurator.lookup(k);
                    if (configurator == null) throw new IllegalStateException("No configurator implementation to manage "+ k);
                    final CNode yaml = config.get(name);
                    final Object value = configurator.configure(yaml);
                    try {
                        logger.info("Setting " + o + '.' + name + " = " + (yaml.isSensibleData() ? "****" : value));
                        attribute.setValue(o, value);
                    } catch (Exception e) {
                        throw new ConfiguratorException(this, "Failed to set attribute " + attribute, e);
                    }
                }
            }
        }

        return o;
    }

    @CheckForNull
    @Override
    public CNode describe(T instance) throws Exception {
        final T ref = target.newInstance();
        return compare(instance, ref);
    }

}
