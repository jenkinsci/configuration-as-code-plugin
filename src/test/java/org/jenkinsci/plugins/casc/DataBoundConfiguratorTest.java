package org.jenkinsci.plugins.casc;

import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.jenkinsci.plugins.casc.model.Scalar;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DataBoundConfiguratorTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void configure_databound() throws Exception {
        Mapping config = new Mapping();
        config.put("foo", "foo");
        config.put("bar", "true");
        config.put("qix", "123");
        config.put("zot", "DataBoundSetter");
        final Foo configured = (Foo) Configurator.lookup(Foo.class).configure(config);
        assertEquals("foo", configured.foo);
        assertEquals(true, configured.bar);
        assertEquals(123, configured.qix);
        assertEquals("DataBoundSetter", configured.zot);
        assertThat(configured.intialized, is(true));
    }


    @Test
    public void exportYaml() throws Exception {
        Foo foo = new Foo("foo", true, 42);
        foo.setZot("zot");
        final Configurator c = Configurator.lookup(Foo.class);
        final CNode node = c.describe(foo);
        assertTrue(node instanceof Mapping);
        Mapping map = (Mapping) node;
        assertEquals(map.get("foo").toString(), "foo");
        assertEquals(map.get("bar").toString(), "true");
        assertEquals(map.get("qix").toString(), "42");
        assertEquals(map.get("zot").toString(), "zot");
        assertFalse(map.containsKey("other"));
    }


    public static class Foo {

        final String foo;
        final boolean bar;
        final int qix;
        String zot;
        String other;
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

        @DataBoundSetter
        public void setOther(String other) {
            this.other = other;
        }

        @PostConstruct
        public void init() {
            this.intialized = true;
        }

        public String getFoo() {
            return foo;
        }

        public boolean isBar() {
            return bar;
        }

        public int getQix() {
            return qix;
        }

        public String getZot() {
            return zot;
        }

        public String getOther() {
            return other;
        }
    }

}
