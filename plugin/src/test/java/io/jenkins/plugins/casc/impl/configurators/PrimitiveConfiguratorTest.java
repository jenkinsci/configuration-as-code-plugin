package io.jenkins.plugins.casc.impl.configurators;

import hudson.model.Node;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.model.Scalar;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class PrimitiveConfiguratorTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public final EnvironmentVariables environment = new EnvironmentVariables();

    @Test
    public void _boolean() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        Configurator c =registry.lookup(boolean.class);
        final Object value = c.configure(new Scalar("true"), new ConfigurationContext(registry));
        assertTrue((Boolean) value);
    }

    @Test
    public void _int() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        Configurator c =registry.lookup(int.class);
        final Object value = c.configure(new Scalar("123"), new ConfigurationContext(registry));
        assertEquals(123, (int) value);
    }

    @Test
    public void _Integer() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        Configurator c =registry.lookup(Integer.class);
        final Object value = c.configure(new Scalar("123"), new ConfigurationContext(registry));
        assertTrue(123 == ((Integer) value).intValue());
    }

    @Test
    public void _string() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        Configurator c =registry.lookup(String.class);
        final Object value = c.configure(new Scalar("abc"), new ConfigurationContext(registry));
        assertEquals("abc", value);
    }

    @Test
    public void _enum() throws Exception {
        // Jenkins do register a StaplerConverter for it.
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        Configurator<Node.Mode> c = registry.lookupOrFail(Node.Mode.class);
        final Node.Mode value = c.configure(new Scalar("NORMAL"), new ConfigurationContext(registry));
        assertEquals(Node.Mode.NORMAL, value);

    }

    @Test
    public void _enum2() throws Exception {
        // No explicit converter set by jenkins
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        Configurator<TimeUnit> c = registry.lookupOrFail(TimeUnit.class);
        final TimeUnit value = c.configure(new Scalar("DAYS"), new ConfigurationContext(registry));
        assertEquals(TimeUnit.DAYS, value);

    }

    @Test
    public void _Integer_env() throws Exception {
        environment.set("ENV_FOR_TEST", "123");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        Configurator c =registry.lookup(Integer.class);
        final Object value = c.configure(new Scalar("${ENV_FOR_TEST}"), new ConfigurationContext(registry));
        assertTrue(123 == ((Integer) value).intValue());
    }

    @Test
    public void _string_env() throws Exception {
        environment.set("ENV_FOR_TEST", "abc");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        Configurator c =registry.lookup(String.class);
        final Object value = c.configure(new Scalar("${ENV_FOR_TEST}"), new ConfigurationContext(registry));
        assertEquals("abc", value);
    }

    @Test
    public void _string_env_default() throws Exception {
        environment.set("NOT_THERE", "abc");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        Configurator c =registry.lookup(String.class);
        final Object value = c.configure(new Scalar("${ENV_FOR_TEST:-unsecured-token}"), new ConfigurationContext(registry));
        assertEquals("unsecured-token", value);
    }

    @Test
    public void _int_env_default() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        Configurator c =registry.lookup(Integer.class);
        final Object value = c.configure(new Scalar("${ENV_FOR_TEST:-123}"), new ConfigurationContext(registry));
        assertEquals(123, value);
    }
}
