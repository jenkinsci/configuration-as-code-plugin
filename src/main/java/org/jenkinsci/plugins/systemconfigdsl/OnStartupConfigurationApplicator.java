package org.jenkinsci.plugins.systemconfigdsl;

import hudson.init.Initializer;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.systemconfigdsl.api.Configurator;
import org.jenkinsci.plugins.systemconfigdsl.error.ConfigurationNotFoundException;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import static hudson.init.InitMilestone.EXTENSIONS_AUGMENTED;
import static hudson.init.InitMilestone.JOB_LOADED;

@SuppressWarnings("unused") // loaded by Jenkins
public class OnStartupConfigurationApplicator {
    private static final Logger LOGGER = Logger.getLogger(OnStartupConfigurationApplicator.class.getName());

    @Initializer(after=EXTENSIONS_AUGMENTED,before=JOB_LOADED)
    public static void init(Jenkins j) throws IOException {
        new OnStartupConfigurationApplicator().apply();
    }

    public void apply() throws IOException {

        final ConfigurationLoader configurationLoader;
        try {
            configurationLoader = new ConfigurationLoader();
        } catch (ConfigurationNotFoundException e) {
            LOGGER.warning("Can't load configuration due to: " + e.getMessage() + ". Will not continue with configuration as code");
            return;
        }

        LOGGER.info("Applying configuration from " + configurationLoader.getConfigurationDir().getAbsolutePath());

        final ServiceImplementationsLoader serviceLoader = new ServiceImplementationsLoader();
        final Map<String,Configurator> configurators = serviceLoader.getConfigurators();

        final Map<String, String> configurations = configurationLoader.loadConfiguration();

        ConfigurationApplicator applicator = new ConfigurationApplicator();
        applicator.applyConfiguration(configurations, configurators, false);
    }
}
