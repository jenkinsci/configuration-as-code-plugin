package io.jenkins.plugins.casc.core;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;

@Extension
public class GlobalNodePropertiesConfigurator extends DataBoundConfigurator<EnvironmentVariablesNodeProperty> {

    public GlobalNodePropertiesConfigurator() {
        this(EnvironmentVariablesNodeProperty.class);
    }

    public GlobalNodePropertiesConfigurator(Class<?> clazz) {
        super(EnvironmentVariablesNodeProperty.class);
    }

    @NonNull
    @Override
    public String getName() {
        return "globalNodeProperties";
    }

    @NonNull
    @Override
    public EnvironmentVariablesNodeProperty configure(CNode c, ConfigurationContext context)
            throws ConfiguratorException {
        return super.configure(c, context);
    }

    @Override
    @CheckForNull
    public CNode describe(EnvironmentVariablesNodeProperty instance, ConfigurationContext context) throws Exception {
        Mapping mapping = new Mapping();
        for (Attribute attribute : getAttributes()) {
            CNode value = attribute.describe(instance, context);
            if (value != null) {
                // Making sure empty variables are part of the export
                value.asSequence().forEach(entry -> {
                    if (entry.asMapping().get("key") != null
                            && entry.asMapping().get("value") != null) {
                        entry.asMapping().get("value").asScalar().setPrintableWhenEmpty(true);
                    }
                });
                mapping.put(attribute.getName(), value);
            }
        }
        return mapping;
    }
}
