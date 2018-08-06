package io.jenkins.plugins.casc.misc;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author lanwen (Kirill Merkushev)
 */
public class JenkinsConfiguredWithCodeRule extends JenkinsRule {
    @Override
    public void before() throws Throwable {
        super.before();
        ConfiguredWithCode configuredWithCode = env.description().getAnnotation(ConfiguredWithCode.class);

        if (Objects.nonNull(configuredWithCode)) {

            final Class<?> clazz = env.description().getTestClass();
            final String[] resource = configuredWithCode.value();

            final List<String> configs = Arrays.asList(resource).stream().map(s -> clazz.getResource(s).toExternalForm())
                    .collect(Collectors.toList());

            try {
                ConfigurationAsCode.get().configure(configs);
            } catch (Throwable t) {
                if (!configuredWithCode.expected().isInstance(t)) {
                    throw new AssertionError("Unexpected exception ", t);
                }
            }
        }
    }
}
