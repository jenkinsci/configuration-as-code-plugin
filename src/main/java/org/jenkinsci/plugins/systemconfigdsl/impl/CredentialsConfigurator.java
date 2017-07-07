package org.jenkinsci.plugins.systemconfigdsl.impl;

import com.google.auto.service.AutoService;
import org.jenkinsci.plugins.systemconfigdsl.api.Configurator;

import java.util.logging.Logger;

@AutoService(Configurator.class)
public class CredentialsConfigurator extends Configurator {

    private static final Logger LOGGER = Logger.getLogger(CredentialsConfigurator.class.getName());
    @Override
    public String getConfigFileSectionName() {
        return "credentials";
    }

    @Override
    public void configure(Object config, boolean dryRun) {
        LOGGER.info("Configuring credentials: " + config.toString());
        if (dryRun == true) {
            LOGGER.info("DryRun: Only print what you will change");
        } else {
            LOGGER.info("Applying configuration...");
        }
    }

    @Override
    public boolean isConfigurationValid(Object config) {
        return true;
    }
}
