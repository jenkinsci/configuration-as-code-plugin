package org.jenkinsci.plugins.casc;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class PrimitiveConfiguratorTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

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
}