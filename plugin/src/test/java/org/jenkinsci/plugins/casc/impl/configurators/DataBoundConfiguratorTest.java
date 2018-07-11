/*
 * Copyright (c) 2018 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.casc.impl.configurators;

import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.PostConstruct;

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
