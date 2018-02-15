package org.jenkinsci.plugins.casc;

import hudson.model.AbstractDescribableImpl;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DataBoundConfiguratorTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void configure_databound() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("foo", "foo");
        config.put("bar", true);
        config.put("qix", 123);
        config.put("zot", "DataBoundSetter");
        final Foo configured = (Foo) Configurator.lookup(Foo.class).configure(config);
        assertEquals("foo", configured.foo);
        assertEquals(true, configured.bar);
        assertEquals(123, configured.qix);
        assertEquals("DataBoundSetter", configured.zot);
        assertTrue( configured.intialized);
    }


    public static class Foo {

        final String foo;
        final boolean bar;
        final int qix;
        String zot;
        boolean intialized;

        @DataBoundConstructor
        public Foo(String foo, boolean bar, int qix) {
            this.foo = foo;
            this.bar = bar;
            this.qix = qix;
        }

        @DataBoundSetter
        public void setZot(String zot) {
            this.zot = zot;
        }

        @PostConstruct
        public void init() {
            this.intialized = true;
        }
    }

}