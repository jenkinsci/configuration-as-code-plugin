package io.jenkins.plugins.casc.impl.configurators;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.GlobalConfiguration;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DescriptorConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("DescriptorConfiguratorTest_camelCase.yml")
    public void configurator_shouldConfigureItselfWhenUsingCamelCase() {
        FooBar descriptor = (FooBar) j.jenkins.getDescriptorOrDie(FooBar.class);
        assertThat(descriptor.getFoo(), equalTo("foo"));
        assertThat(descriptor.getBar(), equalTo("bar"));
    }

    @Test
    @ConfiguredWithCode("DescriptorConfiguratorTest_lowerCase.yml")
    public void configurator_shouldConfigureItselfWhenUsingLoweCase() {
        FooBar descriptor = (FooBar) j.jenkins.getDescriptorOrDie(FooBar.class);
        assertThat(descriptor.getFoo(), equalTo("foo"));
        assertThat(descriptor.getBar(), equalTo("bar"));
    }

    @Test
    @ConfiguredWithCode("DescriptorConfiguratorTest_extendedClass.yml")
    public void configurator_shouldConfigureExtendedClass() {
        Config config = (Config) j.jenkins.getDescriptorOrDie(Config.class);
        assertNotNull(config);

        assertEquals("foo", config.parent.name);
        assertThat(config.parent, Matchers.instanceOf(Child.class));
        Child child = (Child) config.parent;

        assertEquals(0.0, child.min, 0.0);
        assertEquals(3.14, child.max, 0.1);
    }

    @Extension
    public static class FooBar extends GlobalConfiguration {
        private String foo;
        private String bar;

        public FooBar() {
        }

        @DataBoundConstructor
        public FooBar(String foo, String bar) {
            this.foo = foo;
            this.bar = bar;
        }

        @NonNull
        public String getFoo() {
            return foo;
        }

        @DataBoundSetter
        public void setFoo(String foo) {
            this.foo = foo;
        }

        @NonNull
        public String getBar() {
            return bar;
        }

        @DataBoundSetter
        public void setBar(String bar) {
            this.bar = bar;
        }
    }

    @Extension
    public static class Config extends GlobalConfiguration {
        private Parent parent;

        public Parent getParent() {
            return parent;
        }

        @DataBoundSetter
        public void setParent(Parent parent) {
            this.parent = parent;
        }
    }

    public static abstract class Parent extends AbstractDescribableImpl<Parent> {
        private final String name;

        public Parent(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static abstract class ParentDescriptor extends Descriptor<Parent> {
    }

    public static class Child extends Parent {
        private final Double min, max;

        @DataBoundConstructor
        public Child(String name, Double min, Double max) {
            super(name);
            this.min = min;
            this.max = max;
        }

        public Double getMin() {
            return min;
        }

        public Double getMax() {
            return max;
        }

        @Extension
        public static class DescriptorImpl extends ParentDescriptor {}
    }
}
