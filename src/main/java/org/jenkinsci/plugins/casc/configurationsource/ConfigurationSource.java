package org.jenkinsci.plugins.casc.configurationsource;

import hudson.ExtensionPoint;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SPI For configurationsource source. All our sources are .yaml|.yml files, it's the method to which we resolve the value and
 * where those values are stored we want to be able to configure.
 */
public abstract class ConfigurationSource implements ExtensionPoint {

    public abstract Map<String, InputStream> listSources() throws IOException;

    public static List<ConfigurationSource> all() {
        return Jenkins.getInstance().getExtensionList(ConfigurationSource.class);
    }

    public static Map<String, InputStream> getConfigurationInputs() throws IOException {
        Map<String, InputStream> is = new HashMap<>();
        for(ConfigurationSource cs : all()) {
            is.putAll(cs.listSources());
        }
        return is;
    }
}
