package io.jenkins.plugins.casc;

import static org.junit.Assert.assertEquals;

import io.jenkins.plugins.casc.misc.EnvVarsRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;

public class YamlCodePointLimitTest {

    private JenkinsRule j;

    private EnvVarsRule env;

    @Rule
    public RuleChain rc = RuleChain.outerRule(env = new EnvVarsRule())
            .around(new RestoreSystemProperties())
            .around(j = new JenkinsRule());

    @Test
    public void testCodePointLimitSetFifty() throws ConfiguratorException {
        System.setProperty(ConfigurationContext.CASC_YAML_CODE_POINT_LIMIT_PROPERTY, "50");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        assertEquals(50 * 1024 * 1024, context.getYamlCodePointLimit());
    }

    @Test
    public void invalidCodePointLimitSetToDefault() throws ConfiguratorException {
        System.setProperty(ConfigurationContext.CASC_YAML_CODE_POINT_LIMIT_PROPERTY, "HELLO");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        assertEquals(3 * 1024 * 1024, context.getYamlCodePointLimit());
    }

    @Test
    public void defaultCodePointLimit() throws ConfiguratorException {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        assertEquals(3 * 1024 * 1024, context.getYamlCodePointLimit());
    }
}
