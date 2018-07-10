package org.jenkinsci.plugins.casc;

import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Scalar;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class EnumConfigurator<T extends Enum<T>> extends Configurator<T> {

    private final Class<T> clazz;

    public EnumConfigurator(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Class getTarget() {
        return clazz;
    }

    @Nonnull
    @Override
    public Set<Attribute> describe() {
        return Collections.EMPTY_SET;
    }

    @Nonnull
    @Override
    public T configure(CNode config) throws ConfiguratorException {
        return Enum.valueOf(clazz, config.asScalar().getValue());
    }

    @Override
    public T check(CNode config) throws ConfiguratorException {
        return configure(config);
    }

    @CheckForNull
    @Override
    public CNode describe(T instance) throws Exception {
        return new Scalar(instance.name());
    }

}
