package io.jenkins.plugins.casc.impl.configurators;

import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.Configurable;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.CNode;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ConfigurableConfigurator<T extends Configurable> extends Configurator<T> {

    private final Class<T> target;

    public ConfigurableConfigurator(Class<T> target) {
        this.target = target;
    }

    @Override
    public Class<T> getTarget() {
        return target;
    }

    @Nonnull
    @Override
    public Set<Attribute> describe() {
        return Collections.emptySet();
    }

    @Nonnull
    @Override
    public T configure(CNode config, ConfigurationContext context) throws ConfiguratorException {
        try {
            final T instance = target.newInstance();
            instance.configure(config);
            return instance;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ConfiguratorException("Cannot instantiate Configurable "+target+" with default constructor", e);
        }
    }

    @Override
    public T check(CNode config, ConfigurationContext context) throws ConfiguratorException {
        try {
            final T instance = target.newInstance();
            instance.check(config);
            return instance;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ConfiguratorException("Cannot instantiate Configurable "+target+" with default constructor", e);
        }
    }

    @CheckForNull
    @Override
    public CNode describe(T instance, ConfigurationContext context) throws Exception {
        return instance.describe();
    }
}
