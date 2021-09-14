package io.jenkins.plugins.casc.impl.configurators;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.Configurable;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.CNode;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ConfigurableConfigurator<T extends Configurable> implements Configurator<T> {

    private final Class<T> target;

    public ConfigurableConfigurator(Class<T> target) {
        this.target = target;
    }

    @Override
    public Class<T> getTarget() {
        return target;
    }

    @NonNull
    @Override
    public Set<Attribute<T,?>> describe() {
        return Collections.emptySet();
    }

    @NonNull
    @Override
    public T configure(CNode config, ConfigurationContext context) throws ConfiguratorException {
        try {
            final T instance = target.getDeclaredConstructor().newInstance();
            instance.configure(config);
            return instance;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new ConfiguratorException("Cannot instantiate Configurable "+target+" with default constructor", e);
        }
    }

    @Override
    public T check(CNode config, ConfigurationContext context) throws ConfiguratorException {
        try {
            final T instance = target.getDeclaredConstructor().newInstance();
            instance.check(config);
            return instance;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new ConfiguratorException("Cannot instantiate Configurable "+target+" with default constructor", e);
        }
    }

    @CheckForNull
    @Override
    public CNode describe(T instance, ConfigurationContext context) throws Exception {
        return instance.describe();
    }
}
