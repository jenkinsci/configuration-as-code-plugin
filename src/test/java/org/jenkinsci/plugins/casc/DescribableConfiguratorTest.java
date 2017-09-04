package org.jenkinsci.plugins.casc;

import hudson.model.AbstractDescribableImpl;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DescribableConfiguratorTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void _databound() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("foo", "foo");
        config.put("bar", true);
        config.put("qix", 123);
        final Foo configured = (Foo) Configurator.lookup(Foo.class).configure(config);
        assertEquals("foo", configured.foo);
        assertEquals(true, configured.bar);
        assertEquals(123, configured.qix);
    }


    public static class Foo extends AbstractDescribableImpl<Foo> {

        final String foo;
        final boolean bar;
        final int qix;

        @DataBoundConstructor
        public Foo(String foo, boolean bar, int qix) {
            this.foo = foo;
            this.bar = bar;
            this.qix = qix;
        }
    }

}