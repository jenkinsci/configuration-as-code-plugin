package org.jenkinsci.plugins.casc;

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

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public final EnvironmentVariables environment = new EnvironmentVariables();

    @Test
    public void _boolean() throws Exception {
        Configurator c = Configurator.lookup(boolean.class);
        final Object value = c.configure("true");
        assertTrue((Boolean) value);
    }

    @Test
    public void _int() throws Exception {
        Configurator c = Configurator.lookup(int.class);
        final Object value = c.configure("123");
        assertEquals(123, (int) value);
    }

    @Test
    public void _Integer() throws Exception {
        Configurator c = Configurator.lookup(Integer.class);
        final Object value = c.configure("123");
        assertTrue(123 == ((Integer) value).intValue());
    }

    @Test
    public void _string() throws Exception {
        Configurator c = Configurator.lookup(String.class);
        final Object value = c.configure("abc");
        assertEquals("abc", value);
    }

    @Test
    public void _Integer_env() throws Exception {
        environment.set("ENV_FOR_TEST", "123");
        Configurator c = Configurator.lookup(Integer.class);
        final Object value = c.configure("${ENV_FOR_TEST}");
        assertTrue(123 == ((Integer) value).intValue());
    }

    @Test
    public void _string_env() throws Exception {
        environment.set("ENV_FOR_TEST", "abc");
        Configurator c = Configurator.lookup(String.class);
        final Object value = c.configure("${ENV_FOR_TEST}");
        assertEquals("abc", value);
    }
}
