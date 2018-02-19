package org.jenkinsci.plugins.casc.misc;

import org.jenkinsci.plugins.casc.ConfigurationAsCode;

/**
 * Loads resource as configuration-as-code
 */
public class TestConfiguration {
    private final String resource;

    private TestConfiguration(String resource) {
        this.resource = resource;
    }

    public static TestConfiguration withCode(String resource) {
        return new TestConfiguration(resource);
    }

    public void configure(Class<?> clazz) {
        try {
            ConfigurationAsCode.configure(clazz.getResourceAsStream(resource));
        } catch (Exception e) {
            throw new IllegalStateException("Can't configure test with " + resource, e);
        }
    }
}
