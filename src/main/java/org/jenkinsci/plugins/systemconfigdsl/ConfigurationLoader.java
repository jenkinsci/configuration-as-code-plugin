package org.jenkinsci.plugins.systemconfigdsl;

import jenkins.model.Jenkins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ConfigurationLoader {
    private static final Logger LOGGER = Logger.getLogger(ConfigurationLoader.class.getName());
    private File configurationDir;

    public ConfigurationLoader() {
        this.configurationDir = new File(Jenkins.getInstance().getRootDir(),"conf");
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
