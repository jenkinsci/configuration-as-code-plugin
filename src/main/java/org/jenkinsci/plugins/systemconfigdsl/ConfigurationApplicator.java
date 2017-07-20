package org.jenkinsci.plugins.systemconfigdsl;

import org.jenkinsci.plugins.systemconfigdsl.api.Configurator;

import java.util.Map;
import java.util.logging.Logger;

public class ConfigurationApplicator {
    private static final Logger LOGGER = Logger.getLogger(ConfigurationApplicator.class.getName());

    public ConfigurationApplicator() {}

    public void applyConfiguration(final Map<String, String> configurations, final Map<String, Configurator> configurators, final boolean dryRun) {
        for (String configurationName: configurations.keySet()) {
            if (configurators.containsKey(configurationName)) {
                configurators.get(configurationName).configure(configurations.get(configurationName), dryRun);
            } else {
                // Would it be better to throw exeception in here?
                // TODO: add printout to UI when calling dryRun from UI
                LOGGER.warning("No configurator found that could handle section " + configurationName + ". Skip it");
            }
        }
    }
}
