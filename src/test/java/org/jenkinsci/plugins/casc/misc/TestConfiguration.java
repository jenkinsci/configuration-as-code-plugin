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

    private TestConfiguration(String resource) {
        this.resource = resource;
    }

    public static TestConfiguration withCode(String resource) {
        return new TestConfiguration(resource);
    }

    public void configure(Class<?> clazz) {
        try (Reader reader = new InputStreamReader(clazz.getResourceAsStream(resource), UTF_8)) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) new Yaml().loadAs(reader, Map.class)).entrySet()) {
                 ConfigurationAsCode.configureWith(entry);
            }
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Can't configure test <%s> with <%s>", clazz, resource), e);
        }
    }
}
