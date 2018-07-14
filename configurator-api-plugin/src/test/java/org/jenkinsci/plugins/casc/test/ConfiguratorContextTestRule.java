package org.jenkinsci.plugins.casc.test;

import org.jenkinsci.plugins.casc.ConfiguratorContext;
import org.jenkinsci.plugins.casc.ConfiguratorListener;
import org.jenkinsci.plugins.casc.settings.ConfiguratorSettings;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test Rule, which mocks the {@link ConfiguratorContext}.
 * @author Oleg Nenashev
 * @since TODO
 */
public class ConfiguratorContextTestRule implements TestRule {

    private static Logger LOGGER = Logger.getLogger(ConfiguratorContextTestRule.class.getName());

    @Override
    public Statement apply(Statement statement, Description description) {

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try(ConfiguratorContext.ConfiguratorContextHolder holder =
                            ConfiguratorContext.withContext(
                                    new ConfiguratorContext(
                                            ConfiguratorSettings.DEFAULTS,
                                            new ConfiguratorListener.LoggerConfiguratorListener(LOGGER, Level.WARNING))
                            )) {
                    statement.evaluate();
                }
            }
        };
    }
}
