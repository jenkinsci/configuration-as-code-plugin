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
    protected T instance(Mapping mapping) throws ConfiguratorException {
        final ExtensionList<T> list = Jenkins.getInstance().getExtensionList(target);
        if (list.size() != 1) {
            throw new ConfiguratorException("Expected a unique instance of extension "+target);
        }
        return (T) list.get(0);
    }

    @CheckForNull
    @Override
    public CNode describe(T instance) throws Exception {
        final T ref = target.newInstance();
        return compare(instance, ref);
    }

}
