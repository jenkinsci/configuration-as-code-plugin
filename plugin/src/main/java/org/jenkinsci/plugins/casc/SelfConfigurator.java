package org.jenkinsci.plugins.casc;

import hudson.Extension;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension(ordinal = Double.MAX_VALUE)
public class SelfConfigurator extends BaseConfigurator<ConfigurationAsCode> implements RootElementConfigurator<ConfigurationAsCode> {

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
        attributes.add(new Attribute<ConfigurationAsCode, String>("version", String.class)
                .getter(t -> t.getVersion().value())
                .setter((t,v)->t.setVersion(ConfigurationAsCode.Version.of(v))));
        attributes.add(new Attribute("deprecation", ConfigurationAsCode.Deprecation.class));
        attributes.add(new Attribute("restricted", ConfigurationAsCode.Restricted.class));
        attributes.add(new Attribute("unknown", ConfigurationAsCode.Unknown.class));
        return attributes;
    }

    @Nonnull
    @Override
    public ConfigurationAsCode configure(CNode config) throws ConfiguratorException {
        final ConfigurationAsCode c = getTargetComponent();
        configure(config.asMapping(), c);
        return c;
    }

    @CheckForNull
    @Override
    public CNode describe(ConfigurationAsCode instance) throws Exception {
        final Mapping mapping = new Mapping();
        mapping.put("version", instance.getVersion().value());
        return mapping;
    }

}


