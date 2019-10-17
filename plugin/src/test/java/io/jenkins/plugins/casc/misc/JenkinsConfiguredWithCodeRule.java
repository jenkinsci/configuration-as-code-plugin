package io.jenkins.plugins.casc.misc;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.core.StringContains;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

/**
 * @author lanwen (Kirill Merkushev)
 */
public class JenkinsConfiguredWithCodeRule extends JenkinsConfiguredRule {

    @Override
    public void before() throws Throwable {
        super.before();
        ConfiguredWithCode configuredWithCode = getConfiguredWithCode();
        if (Objects.nonNull(configuredWithCode)) {

            final Class<?> clazz = env.description().getTestClass();
            final String[] resource = configuredWithCode.value();

            final List<String> configs = Arrays.stream(resource)
                .map(s -> clazz.getResource(s).toExternalForm())
                .collect(Collectors.toList());

            try {
                ConfigurationAsCode.get().configure(configs);
            } catch (Throwable t) {
                if (!configuredWithCode.expected().isInstance(t)) {
                    throw new AssertionError("Unexpected exception ", t);
                } else {
                    if (!StringUtils.isBlank(configuredWithCode.message())) {
                        boolean match = new StringContains(configuredWithCode.message())
                            .matches(t.getMessage());
                        if (!match) {
                            throw new AssertionError(
                                "Exception did not contain the expected string: "
                                    + configuredWithCode.message() + "\nMessage was:\n" + t
                                    .getMessage());
                        }
                    }
                }
            }
        }
    }

    private ConfiguredWithCode getConfiguredWithCode() {
        ConfiguredWithCode configuredWithCode = env.description()
            .getAnnotation(ConfiguredWithCode.class);
        if (Objects.nonNull(configuredWithCode)) {
            return configuredWithCode;
        }
        for (Field field : env.description().getTestClass().getFields()) {
            if (field.isAnnotationPresent(ConfiguredWithCode.class)) {
                int m = field.getModifiers();
                Class<?> clazz = field.getType();
                if (isPublic(m) && isStatic(m) &&
                    clazz.isAssignableFrom(JenkinsConfiguredWithCodeRule.class)) {
                    configuredWithCode = field.getAnnotation(ConfiguredWithCode.class);
                    if (Objects.nonNull(configuredWithCode)) {
                        return configuredWithCode;
                    }
                } else {
                    throw new IllegalStateException(
                        "Field must be public static JenkinsConfiguredWithCodeRule");
                }
            }
        }
        return null;
    }
}
