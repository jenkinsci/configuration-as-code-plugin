package io.jenkins.plugins.casc.misc;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.core.StringContains;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

/**
 * @author v1v (Victor Martinez)
 */
public class JenkinsConfiguredWithReadmeRule extends JenkinsConfiguredRule {

    @Override
    public void before() throws Throwable {
        super.before();
        ConfiguredWithReadme configuredWithReadme = getConfiguredWithReadme();
        if (Objects.nonNull(configuredWithReadme)) {

            final Class<?> clazz = env.description().getTestClass();
            final String[] resource = configuredWithReadme.value();

            // TODO: transform from `value` to Code blocks

            final List<String> configs = Arrays.stream(resource)
                .map(s -> clazz.getClassLoader().getResource(s).toExternalForm())
                .collect(Collectors.toList());

            try {
                ConfigurationAsCode.get().configure(configs);
            } catch (Throwable t) {
                if (!configuredWithReadme.expected().isInstance(t)) {
                    throw new AssertionError("Unexpected exception ", t);
                } else {
                    if (!StringUtils.isBlank(configuredWithReadme.message())) {
                        boolean match = new StringContains(configuredWithReadme.message())
                            .matches(t.getMessage());
                        if (!match) {
                            throw new AssertionError(
                                "Exception did not contain the expected string: "
                                    + configuredWithReadme.message() + "\nMessage was:\n" + t
                                    .getMessage());
                        }
                    }
                }
            }
        }
    }

    private ConfiguredWithReadme getConfiguredWithReadme() {
        ConfiguredWithReadme configuredWithReadme = env.description()
            .getAnnotation(ConfiguredWithReadme.class);
        if (Objects.nonNull(configuredWithReadme)) {
            return configuredWithReadme;
        }
        for (Field field : env.description().getTestClass().getFields()) {
            if (field.isAnnotationPresent(ConfiguredWithReadme.class)) {
                int m = field.getModifiers();
                Class<?> clazz = field.getType();
                if (isPublic(m) && isStatic(m) &&
                    clazz.isAssignableFrom(JenkinsConfiguredWithReadmeRule.class)) {
                    configuredWithReadme = field.getAnnotation(ConfiguredWithReadme.class);
                    if (Objects.nonNull(configuredWithReadme)) {
                        return configuredWithReadme;
                    }
                } else {
                    throw new IllegalStateException(
                        "Field must be public static JenkinsConfiguredWithReadmeRule");
                }
            }
        }
        return null;
    }
}
