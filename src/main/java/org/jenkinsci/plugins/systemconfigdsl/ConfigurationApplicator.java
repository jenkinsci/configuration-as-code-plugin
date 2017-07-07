package org.jenkinsci.plugins.systemconfigdsl;

import org.jenkinsci.plugins.systemconfigdsl.api.Configurator;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ConfigurationApplicator {
    private static final Logger LOGGER = Logger.getLogger(ConfigurationApplicator.class.getName());

    public ConfigurationApplicator() {}

    public void applyConfiguration(List<Object> configuration, Map<String, Configurator> configurators, boolean dryRun) {

    }
}
