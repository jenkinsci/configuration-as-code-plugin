package org.jenkinsci.plugins.casc.impl.configurators;

import hudson.ExtensionList;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.BaseConfigurator;
import org.jenkinsci.plugins.casc.ConfigurationContext;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import java.util.logging.Logger;

/**
 * A generic {@link Configurator} for {@link hudson.Extension} singletons
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Restricted(NoExternalUse.class)
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
    protected T instance(Mapping mapping, ConfigurationContext context) throws ConfiguratorException {
        final ExtensionList<T> list = Jenkins.getInstance().getExtensionList(target);
        if (list.size() != 1) {
            throw new ConfiguratorException("Expected a unique instance of extension "+target);
        }
        return (T) list.get(0);
    }

    @CheckForNull
    @Override
    public CNode describe(T instance, ConfigurationContext context) throws Exception {
        final T ref = target.newInstance();
        return compare(instance, ref, context);
    }

}
