package org.jenkinsci.plugins.casc.impl.configurators;

import hudson.model.Node;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.model.Scalar;
import org.jenkinsci.plugins.casc.test.ConfiguratorContextTestRule;
import org.junit.Rule;
import org.junit.Test;
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
    public ConfiguratorContextTestRule context = new ConfiguratorContextTestRule();

    @Test
    public void _boolean() throws Exception {
        Configurator c = Configurator.lookup(boolean.class);
        final Object value = c.configure(new Scalar("true"));
        assertTrue((Boolean) value);
    }

    @Test
    public void _int() throws Exception {
        Configurator c = Configurator.lookup(int.class);
        final Object value = c.configure(new Scalar("123"));
        assertEquals(123, (int) value);
    }

    @Test
    public void _Integer() throws Exception {
        Configurator c = Configurator.lookup(Integer.class);
        final Object value = c.configure(new Scalar("123"));
        assertTrue(123 == ((Integer) value).intValue());
    }

    @Test
    public void _string() throws Exception {
        Configurator c = Configurator.lookup(String.class);
        final Object value = c.configure(new Scalar("abc"));
        assertEquals("abc", value);
    }

    @Test
    public void _enum() throws Exception {
        // Jenkins do register a StaplerConverter for it.
        Configurator<Node.Mode> c = Configurator.lookupOrFail(Node.Mode.class);
        final Node.Mode value = c.configure(new Scalar("NORMAL"));
        assertEquals(Node.Mode.NORMAL, value);

    }

    @Test
    public void _enum2() throws Exception {
        // No explicit converter set by jenkins
        Configurator<TimeUnit> c = Configurator.lookupOrFail(TimeUnit.class);
        final TimeUnit value = c.configure(new Scalar("DAYS"));
        assertEquals(TimeUnit.DAYS, value);

    }
}
