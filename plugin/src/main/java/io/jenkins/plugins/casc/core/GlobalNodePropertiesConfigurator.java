package io.jenkins.plugins.casc.core;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.impl.attributes.MultivaluedAttribute;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Sequence;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Extension
public class GlobalNodePropertiesConfigurator extends BaseConfigurator<EnvironmentVariablesNodeProperty> {

    @NonNull
    @Override
    public String getName() {
        return "globalNodeProperties";
    }

    @Override
    protected EnvironmentVariablesNodeProperty instance(Mapping mapping, ConfigurationContext context)
            throws ConfiguratorException {
        List<Entry> vars = getVarsAsList(mapping);
        return new EnvironmentVariablesNodeProperty(vars);
    }

    @Override
    public Class<EnvironmentVariablesNodeProperty> getTarget() {
        return EnvironmentVariablesNodeProperty.class;
    }

    @NonNull
    @Override
    public Set<Attribute<EnvironmentVariablesNodeProperty, ?>> describe() {
        Set<Attribute<EnvironmentVariablesNodeProperty, ?>> attrs = super.describe();
        attrs.add(new MultivaluedAttribute<EnvironmentVariablesNodeProperty, EnvironmentVariablesNodeProperty.Entry>(
                "env", Entry.class));
        return attrs;
    }

    @NonNull
    @Override
    public EnvironmentVariablesNodeProperty configure(CNode c, ConfigurationContext context)
            throws ConfiguratorException {
        Mapping mapping = c.asMapping();
        List<Entry> variables = getVarsAsList(mapping);
        return new EnvironmentVariablesNodeProperty(variables);
    }

    @CheckForNull
    public CNode describe(EnvironmentVariablesNodeProperty instance, ConfigurationContext context) throws Exception {
        Mapping mapping = new Mapping();
        for (Attribute attribute : getAttributes()) {
            CNode value = attribute.describe(instance, context);
            if (value != null) {
                Sequence values = new Sequence();
                value.asSequence().stream().forEach(values::add);
                mapping.put(attribute.getName(), values);
            }
        }
        return mapping;
    }

    private List<Entry> getVarsAsList(Mapping m) {
        List<Entry> result = new ArrayList<>();
        if (m.get("env") != null) {
            result = m.get("env").asSequence().stream()
                    .map(pair -> {
                        if (pair.asMapping().get("key") == null) {
                            return null;
                        }
                        final String key =
                                pair.asMapping().get("key").asScalar().getValue();
                        final String value = pair.asMapping().get("value") == null
                                ? ""
                                : pair.asMapping().get("value").asScalar().getValue();

                        return new Entry(key, value);
                    })
                    .toList();
        }
        return result;
    }
}
