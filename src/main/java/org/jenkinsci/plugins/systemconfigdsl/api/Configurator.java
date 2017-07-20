package org.jenkinsci.plugins.systemconfigdsl.api;

import com.google.gson.Gson;

public abstract class Configurator {

    public abstract String getConfigFileSectionName();

    public abstract void configure(final String config, final boolean dryRun);

    public abstract boolean isConfigurationValid(final String config);

    protected ConfigurationDescription parseConfiguration(final String config, final Class<? extends ConfigurationDescription> configurationDescriptionClass) {
        Gson gson = new Gson();
        return gson.fromJson(config, configurationDescriptionClass);
    }
}