package org.jenkinsci.plugins.casc.misc;

import org.jvnet.hudson.test.JenkinsRule;

import java.util.Objects;

/**
 * @author lanwen (Kirill Merkushev)
 */
public class JenkinsConfiguredWithCodeRule extends JenkinsRule {
    @Override
    public void before() throws Throwable {
        super.before();
        ConfiguredWithCode configuredWithCode = env.description().getAnnotation(ConfiguredWithCode.class);

        if (Objects.nonNull(configuredWithCode)) {
            TestConfiguration.withCode(configuredWithCode.value()).configure(env.description().getTestClass());
        }
    }
}
