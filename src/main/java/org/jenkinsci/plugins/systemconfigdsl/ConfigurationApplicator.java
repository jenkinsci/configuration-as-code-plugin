package org.jenkinsci.plugins.systemconfigdsl;

import org.jenkinsci.plugins.systemconfigdsl.api.Configurator;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ConfigurationApplicator {
    private static final Logger LOGGER = Logger.getLogger(ConfigurationApplicator.class.getName());

    public ConfigurationApplicator() {}

    public void applyConfiguration(List<Object> configurations, Map<String, Configurator> configurators, boolean dryRun) {
        for (Object configuration: configurations) {
            for (Object section: ((Map) configuration).keySet()) {
                if (configurators.containsKey(section)) {
                    configurators.get(section).configure(configuration, dryRun);
                } else {
                    // Would it be better to throw exeception in here?
                    // TODO: add printout to UI when calling dryRun from UI
                    LOGGER.warning("No configurator found that could handle section " + section.toString() + ". Skip it");
                }
            }
        }

    }
}
