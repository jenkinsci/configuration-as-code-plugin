package io.jenkins.plugins.casc.impl.configurators;

import hudson.util.Secret;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.SecretSourceResolver;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Scalar;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.Stapler;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Restricted(NoExternalUse.class)
public class PrimitiveConfigurator implements Configurator {
    private final Class target;

    public PrimitiveConfigurator(Class clazz) {
        this.target = clazz;
    }

    @Override
    public Class getTarget() {
        return target;
    }

    @Nonnull
    @Override
    public Set<Attribute> describe() {
        return Collections.emptySet();
    }

    @Nonnull
    @Override
    public Object configure(CNode config, ConfigurationContext context) throws ConfiguratorException {
        return Stapler.lookupConverter(target).convert(target, SecretSourceResolver.resolve(context, config.asScalar().toString()));
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
        if (instance instanceof Secret) {
            return new Scalar(((Secret) instance).getEncryptedValue());
        }
        if (target.isEnum()) {
            return new Scalar((Enum) instance);
        }

        return new Scalar(String.valueOf(instance));
    }

    @Nonnull
    @Override
    public List<Configurator> getConfigurators(ConfigurationContext context) {
        return Collections.emptyList();
    }
}
