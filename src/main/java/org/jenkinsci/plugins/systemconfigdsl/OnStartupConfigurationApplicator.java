package org.jenkinsci.plugins.systemconfigdsl;

import hudson.init.Initializer;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.systemconfigdsl.api.Configurator;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static hudson.init.InitMilestone.EXTENSIONS_AUGMENTED;
import static hudson.init.InitMilestone.JOB_LOADED;

public class OnStartupConfigurationApplicator {
    private static final Logger LOGGER = Logger.getLogger(OnStartupConfigurationApplicator.class.getName());

    @Initializer(after=EXTENSIONS_AUGMENTED,before=JOB_LOADED)
    public static void init(Jenkins j) throws IOException {
        new OnStartupConfigurationApplicator().apply();
    }

    public void apply() throws IOException {
        final ConfigurationLoader configurationLoader = new ConfigurationLoader();

        LOGGER.info("Applying configuration from " + configurationLoader.getConfigurationDir().getAbsolutePath());

        final ServiceImplementationsLoader serviceLoader = new ServiceImplementationsLoader();
        final Map<String,Configurator> configurators = serviceLoader.getConfigurators();

        final List<Object> configuration= configurationLoader.loadConfiguration();

        ConfigurationApplicator applicator = new ConfigurationApplicator();
        applicator.applyConfiguration(configuration, configurators, false);
    }
}
