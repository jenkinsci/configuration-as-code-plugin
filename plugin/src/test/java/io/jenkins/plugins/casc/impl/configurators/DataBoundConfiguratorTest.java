package io.jenkins.plugins.casc.impl.configurators;

import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import javax.annotation.PostConstruct;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DataBoundConfiguratorTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Test
    public void configure_databound() throws Exception {
        Mapping config = new Mapping();
        config.put("foo", "foo");
        config.put("bar", "true");
        config.put("qix", "123");
        config.put("zot", "DataBoundSetter");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final Foo configured = (Foo) registry.lookupOrFail(Foo.class).configure(config, new ConfigurationContext(registry));
        assertEquals("foo", configured.foo);
        assertTrue(configured.bar);
        assertEquals(123, configured.qix);
        assertEquals("DataBoundSetter", configured.zot);
        assertThat(configured.initialized, is(true));
    }


    @Test
    public void exportYaml() throws Exception {
        Foo foo = new Foo("foo", true, 42);
        foo.setZot("zot");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final Configurator c = registry.lookupOrFail(Foo.class);
        final ConfigurationContext context = new ConfigurationContext(registry);
        final CNode node = c.describe(foo, context);
        assertNotNull(node);
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
        boolean initialized;

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
            this.initialized = true;
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
