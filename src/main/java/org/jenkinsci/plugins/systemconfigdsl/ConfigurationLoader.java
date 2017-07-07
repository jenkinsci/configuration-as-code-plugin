package org.jenkinsci.plugins.systemconfigdsl;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import jenkins.model.Jenkins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
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

    public List<Object> loadConfiguration() throws YamlException, FileNotFoundException {
        LOGGER.info("Loading configuration from " + configurationDir.getAbsolutePath());
        final List<Object> configuration = new ArrayList<>();

        File[] files = this.configurationDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".yml");
            }
        });

        for (File file : files) {
            YamlReader reader = new YamlReader(new FileReader(file));
            configuration.add(reader.read());
            LOGGER.info("Found and parsed configuration file " + file.getAbsolutePath());
        }
        return configuration;
    }
}
