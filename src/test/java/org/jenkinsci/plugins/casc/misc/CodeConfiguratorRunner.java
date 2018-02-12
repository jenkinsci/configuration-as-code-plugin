package org.jenkinsci.plugins.casc.misc;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Objects;

/**
 * Reads value of {@link ConfiguredWithCode} and evaluates config same way as plugin.
 * Requires Jenkins instance already up and running
 *
 * @see ConfiguredWithCode
 */
public class CodeConfiguratorRunner implements TestRule {
    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                ConfiguredWithCode configuredWithCode = description.getAnnotation(ConfiguredWithCode.class);

                if (Objects.nonNull(configuredWithCode)) {
                    new TestConfiguration(configuredWithCode.value()).configure(description.getTestClass());
                }

                base.evaluate();
            }
        };
    }
}
