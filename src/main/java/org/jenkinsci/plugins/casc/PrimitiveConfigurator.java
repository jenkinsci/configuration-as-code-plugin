package org.jenkinsci.plugins.casc;

import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Scalar;
import org.kohsuke.stapler.Stapler;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class PrimitiveConfigurator extends Configurator {
    private final Class target;

    public PrimitiveConfigurator(Class clazz) {
        this.target = clazz;
    }

    @Override
    public Class getTarget() {
        return target;
    }

    @Override
    public Set<Attribute> describe() {
        return Collections.EMPTY_SET;
    }

    @Override
    public Object configure(CNode config) throws ConfiguratorException {
        return Stapler.lookupConverter(target).convert(target, config);
    }

    @CheckForNull
    @Override
    public CNode describe(Object instance) {
        return new Scalar(String.valueOf(instance));
    }

    @Override
    public List<Configurator> getConfigurators() {
        return Collections.EMPTY_LIST;
    }
}
