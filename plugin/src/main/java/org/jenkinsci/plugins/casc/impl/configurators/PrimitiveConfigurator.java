package org.jenkinsci.plugins.casc.impl.configurators;

import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.ConfigurationContext;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.SecretSource;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Scalar;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
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
@Restricted(NoExternalUse.class)
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
    public Object configure(CNode config, ConfigurationContext context) throws ConfiguratorException {

        final String value = config.asScalar().getValue();
        String s = value;
        Optional<String> r = SecretSource.requiresReveal(value);
        if (r.isPresent()) {
            final String expr = r.get();
            Optional<String> reveal = Optional.empty();
            for (SecretSource secretSource : context.getSecretSources()) {
                try {
                    reveal = secretSource.reveal(expr);
                } catch (IOException ex) {
                    throw new RuntimeException("Cannot reveal secret source for variable with key: " + s, ex);
                }
                if (reveal.isPresent()) {
                    s = reveal.get();
                    break;
                }
            }

            Optional<String> defaultValue = SecretSource.defaultValue(value);
            if (defaultValue.isPresent() && !reveal.isPresent()) {
                s = defaultValue.get();
            }

            if (!reveal.isPresent() && !defaultValue.isPresent()) {
                throw new RuntimeException("Unable to reveal variable with key: " + s);
            }
        }

        return Stapler.lookupConverter(target).convert(target, s);
    }

    @Override
    public Object check(CNode config, ConfigurationContext context) throws ConfiguratorException {
        return configure(config, context);
    }

    @CheckForNull
    @Override
    public CNode describe(Object instance, ConfigurationContext context) {

        if (instance == null) return null;

        if (instance instanceof Number) {
            return new Scalar((Number) instance);
        }
        if (instance instanceof Boolean) {
            return new Scalar((Boolean) instance);
        }
        if (target.isEnum()) {
            return new Scalar((Enum) instance);
        }

        return new Scalar(String.valueOf(instance));
    }

    @Override
    public List<Configurator> getConfigurators(ConfigurationContext context) {
        return Collections.EMPTY_LIST;
    }
}
