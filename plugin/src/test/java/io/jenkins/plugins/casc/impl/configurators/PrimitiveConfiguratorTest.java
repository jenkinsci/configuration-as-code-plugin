package io.jenkins.plugins.casc.impl.configurators;

import hudson.model.Node;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.model.Scalar;
import java.util.concurrent.TimeUnit;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class PrimitiveConfiguratorTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Rule
    public final EnvironmentVariables environment = new EnvironmentVariables();

    private static ConfiguratorRegistry registry;
    private static ConfigurationContext context;

    @BeforeClass
    public static void setup() {
        registry = ConfiguratorRegistry.get();
        context = new ConfigurationContext(registry);
    }

    @Test
    public void _boolean() throws Exception {
        Configurator c = registry.lookupOrFail(boolean.class);
        final Object value = c.configure(new Scalar("true"), context);
        assertTrue((Boolean) value);
    }

    @Test
    public void _int() throws Exception {
        Configurator c = registry.lookupOrFail(int.class);
        final Object value = c.configure(new Scalar("123"), context);
        assertEquals(123, (int) value);
    }

    @Test
    public void _Integer() throws Exception {
        Configurator c = registry.lookupOrFail(Integer.class);
        final Object value = c.configure(new Scalar("123"), context);
        assertTrue(123 == (Integer) value);
    }

    @Test
    public void _string() throws Exception {
        Configurator c = registry.lookupOrFail(String.class);
        final Object value = c.configure(new Scalar("abc"), context);
        assertEquals("abc", value);
    }

    @Test
    public void _enum() throws Exception {
        // Jenkins do register a StaplerConverter for it.
        Configurator<Node.Mode> c = registry.lookupOrFail(Node.Mode.class);
        final Node.Mode value = c.configure(new Scalar("NORMAL"), context);
        assertEquals(Node.Mode.NORMAL, value);

    }

    @Test
    public void _enum2() throws Exception {
        // No explicit converter set by jenkins
        Configurator<TimeUnit> c = registry.lookupOrFail(TimeUnit.class);
        final TimeUnit value = c.configure(new Scalar("DAYS"), context);
        assertEquals(TimeUnit.DAYS, value);

    }

    @Test
    public void _Integer_env() throws Exception {
        environment.set("ENV_FOR_TEST", "123");
        Configurator c = registry.lookupOrFail(Integer.class);
        final Object value = c.configure(new Scalar("${ENV_FOR_TEST}"), context);
        assertTrue(123 == (Integer) value);
    }

    @Test
    public void _string_env() throws Exception {
        environment.set("ENV_FOR_TEST", "abc");
        Configurator c = registry.lookupOrFail(String.class);
        final Object value = c.configure(new Scalar("${ENV_FOR_TEST}"), context);
        assertEquals("abc", value);
    }

    @Test
    public void _string_env_default() throws Exception {
        environment.set("NOT_THERE", "abc");
        Configurator c = registry.lookupOrFail(String.class);
        final Object value = c.configure(new Scalar("${ENV_FOR_TEST:-unsecured-token}"), context);
        assertEquals("unsecured-token", value);
    }

    @Test
    public void _int_env_default() throws Exception {
        Configurator c = registry.lookupOrFail(Integer.class);
        final Object value = c.configure(new Scalar("${ENV_FOR_TEST:-123}"), context);
        assertEquals(123, value);
    }
}
