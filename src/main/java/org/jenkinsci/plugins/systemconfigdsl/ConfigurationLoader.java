package org.jenkinsci.plugins.systemconfigdsl;

import jenkins.model.Jenkins;
import org.jenkinsci.plugins.systemconfigdsl.error.ConfigurationNotFoundException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ConfigurationLoader {
    private static final Logger LOGGER = Logger.getLogger(ConfigurationLoader.class.getName());
    private static final String CONF_ENV_VAR = "JENKINS_CONF";
    private static final String CONF_SYS_PROP = "JENKINS_CONF";
    private File configurationDir;

    public ConfigurationLoader() throws ConfigurationNotFoundException {
        // Try to read location from system properties
        String confPath = System.getProperty(CONF_SYS_PROP, null);
        // If not yet defined look for env variable
        if (confPath == null && System.getenv().getOrDefault(CONF_ENV_VAR, null) != null) {
            confPath = System.getenv().get(CONF_ENV_VAR);
        }
        // If still not defined default to JENKINS_HOME/conf
        if (confPath == null) {
            confPath = Jenkins.getInstance().getRootDir().getPath().toString() + "/conf";
        }
        LOGGER.info("Jenkins configuration location is set to " + confPath);
        this.configurationDir = new File(confPath);
        if (! this.configurationDir.exists() || ! this.configurationDir.isDirectory()) {
            throw new ConfigurationNotFoundException(confPath + " doesn't exists or not a directory");
        }
    }

    public ConfigurationLoader(final File configurationDir) {
        this.configurationDir = configurationDir;
    }

    public File getConfigurationDir() {
        return configurationDir;
    }

    public Map<String, String> loadConfiguration() throws IOException {
        LOGGER.info("Loading configuration from " + configurationDir.getAbsolutePath());
        final HashMap<String, String> configurations = new HashMap<>();

        for (File file : this.configurationDir.listFiles((dir, name) -> name.endsWith(".json"))) {
            configurations.put(file.getName().replace(".json", ""), new String(Files.readAllBytes(file.toPath())));
            LOGGER.info("Found and parsed configuration file: " + file.getAbsolutePath());
            LOGGER.info("Content: " + configurations.get(file.getName().replace(".json", "")));
        }
        return configurations;
    }
}
