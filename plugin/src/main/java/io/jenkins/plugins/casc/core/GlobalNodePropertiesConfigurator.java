package io.jenkins.plugins.casc.core;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.EnvironmentVariablesNodeProperty.Entry;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.ArrayList;
import java.util.List;

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
        Mapping mapping = c.asMapping();
        List<Entry> variables = getVarsAsList(mapping, context);
        return new EnvironmentVariablesNodeProperty(variables);
    }

    private List<Entry> getVarsAsList(Mapping m, ConfigurationContext context) {
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
                                : context.getSecretSourceResolver().resolve(pair.asMapping().get("value").asScalar().getValue());

                        return new Entry(key, value);
                    })
                    .toList();
        }
        return result;
    }
}
