package org.jenkinsci.plugins.casc.misc;

import org.jenkinsci.plugins.casc.ConfigurationAsCode;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Loads resource as configuration-as-code
 */
public class TestConfiguration {
    private final String resource;

    public TestConfiguration(String resource) {
        this.resource = resource;
    }

    public void configure(Class<?> clazz) {
        try {
            ConfigurationAsCode.configure(clazz.getResourceAsStream(resource));
        } catch (Exception e) {
            throw new IllegalStateException("Can't configure test with " + resource, e);
        }
    }
}
