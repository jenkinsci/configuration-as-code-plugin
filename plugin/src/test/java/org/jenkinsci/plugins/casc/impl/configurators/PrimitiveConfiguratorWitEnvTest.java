package org.jenkinsci.plugins.casc.impl.configurators;

import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.model.Scalar;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.jvnet.hudson.test.For;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@For(PrimitiveConfigurator.class)
public class PrimitiveConfiguratorWitEnvTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public final EnvironmentVariables environment = new EnvironmentVariables();

    @Test
    public void _Integer_env() throws Exception {
        environment.set("ENV_FOR_TEST", "123");
        Configurator c = Configurator.lookup(Integer.class);
        final Object value = c.configure(new Scalar("${ENV_FOR_TEST}"));
        Assert.assertNotNull(value);
        assertTrue(123 == ((Integer) value).intValue());
    }

    @Test
    public void _string_env() throws Exception {
        environment.set("ENV_FOR_TEST", "abc");
        Configurator c = Configurator.lookup(String.class);
        final Object value = c.configure(new Scalar("${ENV_FOR_TEST}"));
        assertEquals("abc", value);
    }

    @Test
    public void _string_env_default() throws Exception {
        environment.set("NOT_THERE", "abc");
        Configurator c = Configurator.lookup(String.class);
        final Object value = c.configure(new Scalar("${ENV_FOR_TEST:-unsecured-token}"));
        assertEquals("unsecured-token", value);
    }

    @Test
    public void _int_env_default() throws Exception {
        Configurator c = Configurator.lookup(Integer.class);
        final Object value = c.configure(new Scalar("${ENV_FOR_TEST:-123}"));
        assertEquals(123, value);
    }
}
