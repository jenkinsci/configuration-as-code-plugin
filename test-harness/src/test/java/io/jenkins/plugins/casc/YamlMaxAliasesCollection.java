package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.EnvVarsRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class YamlMaxAliasesCollection {

    private JenkinsRule j;

    private EnvVarsRule env;

    @Rule
    public RuleChain rc = RuleChain.outerRule(env = new EnvVarsRule())
        .around(new RestoreSystemProperties())
        .around(j = new JenkinsRule());

    @Test
    public void testAMaxOfOneEnv() {
        env.set(ConfigurationContext.CASC_YAML_MAX_ALIASES_ENV, "1");
        ConfiguratorException e = assertThrows(ConfiguratorException.class, () ->
                ConfigurationAsCode.get()
                    .configure(getClass().getResource("maxAliasesLimit.yml").toExternalForm()));
        assertEquals("Number of aliases for non-scalar nodes exceeds the specified max=1\n"
            + "You can increase the maximum by setting an environment variable or property\n"
            + "  ENV: CASC_YAML_MAX_ALIASES=\"100\"\n"
            + "  PROPERTY: -Dcasc.yaml.max.aliases=\"100\"", e.getCause().getMessage().replaceAll("\\r\\n?", "\n"));
    }

    @Test
    public void testAMaxOfOneProp() {
        System.setProperty(ConfigurationContext.CASC_YAML_MAX_ALIASES_PROPERTY, "1");
        ConfiguratorException e = assertThrows(ConfiguratorException.class, () ->
                ConfigurationAsCode.get()
                    .configure(getClass().getResource("maxAliasesLimit.yml").toExternalForm()));
        assertEquals("Number of aliases for non-scalar nodes exceeds the specified max=1\n"
            + "You can increase the maximum by setting an environment variable or property\n"
            + "  ENV: CASC_YAML_MAX_ALIASES=\"100\"\n"
            + "  PROPERTY: -Dcasc.yaml.max.aliases=\"100\"", e.getCause().getMessage().replaceAll("\\r\\n?", "\n"));
    }

    @Test
    public void testAMaxOfTwoEnv() throws ConfiguratorException {
        env.set(ConfigurationContext.CASC_YAML_MAX_ALIASES_ENV, "2");
        ConfigurationAsCode.get().configure(getClass().getResource("maxAliasesLimit.yml").toExternalForm());
        final Jenkins jenkins = Jenkins.get();
        assertEquals(2, jenkins.getNodes().size());
        assertEquals("static-agent1", jenkins.getNode("static-agent1").getNodeName());
        assertEquals("static-agent2", jenkins.getNode("static-agent2").getNodeName());
    }

    @Test
    public void testAMaxOfTwoProp() throws ConfiguratorException {
        System.setProperty(ConfigurationContext.CASC_YAML_MAX_ALIASES_PROPERTY, "2");
        ConfigurationAsCode.get().configure(getClass().getResource("maxAliasesLimit.yml").toExternalForm());
        final Jenkins jenkins = Jenkins.get();
        assertEquals(2, jenkins.getNodes().size());
        assertEquals("static-agent1", jenkins.getNode("static-agent1").getNodeName());
        assertEquals("static-agent2", jenkins.getNode("static-agent2").getNodeName());
    }

    @Test
    public void invalidInputShouldDefaultTo50() throws ConfiguratorException {
        System.setProperty(ConfigurationContext.CASC_YAML_MAX_ALIASES_PROPERTY, "HELLO");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        assertEquals(50, context.getYamlMaxAliasesForCollections());
        ConfigurationAsCode.get().configure(getClass().getResource("maxAliasesLimit.yml").toExternalForm());
        final Jenkins jenkins = Jenkins.get();
        assertEquals(2, jenkins.getNodes().size());
        assertEquals("static-agent1", jenkins.getNode("static-agent1").getNodeName());
        assertEquals("static-agent2", jenkins.getNode("static-agent2").getNodeName());
    }
}
