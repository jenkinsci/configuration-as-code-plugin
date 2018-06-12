package org.jenkinsci.plugins.casc;

import hudson.Extension;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension(ordinal = Double.MAX_VALUE)
public class SelfConfigurator extends Configurator<ConfigurationAsCode> implements RootElementConfigurator<ConfigurationAsCode> {

    @Override
    public String getName() {
        return "configuration-as-code";
    }

    @Override
    public Class<ConfigurationAsCode> getTarget() {
        return ConfigurationAsCode.class;
    }

    @Override
    public ConfigurationAsCode getTargetComponent() {
        return ConfigurationAsCode.get();
    }


    @Nonnull
    @Override
    public Set<Attribute> describe() {
        Set<Attribute> attributes = new HashSet<>();
        attributes.add(new Attribute<ConfigurationAsCode, String>("version", String.class));
        return attributes;
    }

    @Nonnull
    @Override
    public ConfigurationAsCode configure(CNode config) throws ConfiguratorException {
        final ConfigurationAsCode t = getTargetComponent();
        final Mapping mapping = config.asMapping();
        t.setVersion(ConfigurationAsCode.Version.of(mapping.getScalarValue("version")));
        return t;
    }

    @CheckForNull
    @Override
    public CNode describe(ConfigurationAsCode instance) throws Exception {
        final Mapping mapping = new Mapping();
        mapping.put("version", instance.getVersion().value());
        return mapping;
    }

}


