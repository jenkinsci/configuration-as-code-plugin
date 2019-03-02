package io.jenkins.plugins.casc.impl.configurators;

import hudson.ExtensionList;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import java.util.logging.Logger;

/**
 * A generic {@link io.jenkins.plugins.casc.Configurator} for {@link hudson.Extension} singletons
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Restricted(NoExternalUse.class)
public class ExtensionConfigurator<T> extends BaseConfigurator<T> {

    private static final Logger LOGGER = Logger.getLogger(ExtensionConfigurator.class.getName());

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
