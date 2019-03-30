package io.jenkins.plugins.casc.impl.configurators;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.GlobalConfiguration;
import org.jenkinsci.Symbol;
import org.junit.Rule;
import org.junit.Test;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Objects;

public class DuplicateKeyDescribableConfiguratorTest {
    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void implementors_shouldNotThrowException() {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        HeteroDescribableConfigurator configurator = Objects.requireNonNull((HeteroDescribableConfigurator) registry.lookup(FooBar.class));

        assertThat(configurator.getImplementors().size(), equalTo(1));
    }

    @Test
    @ConfiguredWithCode("DuplicateKeyDescribableConfigure.yml")
    public void configure_shouldNotThrowException() {
        FooBarGlobalConfiguration descriptor = (FooBarGlobalConfiguration) j.jenkins.getDescriptor(FooBarGlobalConfiguration.class);
        FooBarOne instance = (FooBarOne) Objects.requireNonNull(descriptor).fooBar;
        assertThat(instance.foo, equalTo("hello"));
        assertThat(instance.bar, equalTo("world"));
    }

    @Extension
    @Symbol("fooBarGlobal")
    public static class FooBarGlobalConfiguration extends GlobalConfiguration {
        private FooBar fooBar;

        public FooBarGlobalConfiguration() {
        }

        @DataBoundConstructor
        public FooBarGlobalConfiguration(FooBar fooBar) {
            this.fooBar = fooBar;
        }

        public FooBar getFooBar() {
            return fooBar;
        }

        @DataBoundSetter
        public void setFooBar(FooBar fooBar) {
            this.fooBar = fooBar;
        }
    }

    public static abstract class FooBar implements Describable<FooBar> {

    }

    public static class FooBarOne extends FooBar {
        private String foo;
        private String bar;

        @DataBoundConstructor
        public FooBarOne(String foo, String bar) {
            this.foo = foo;
            this.bar = bar;
        }

        @Override
        public Descriptor<FooBar> getDescriptor() {
            return new DescriptorImpl();
        }

        public String getFoo() {
            return foo;
        }

        @DataBoundSetter
        public void setFoo(String foo) {
            this.foo = foo;
        }

        public String getBar() {
            return bar;
        }

        @DataBoundSetter
        public void setBar(String bar) {
            this.bar = bar;
        }

        @Extension
        @Symbol("fooBarInner")
        public static class DescriptorImpl extends Descriptor<FooBar> {

        }
    }

    public static class FooBarTwo extends FooBar {
        @Override
        public Descriptor<FooBar> getDescriptor() {
            return new DescriptorImpl();
        }

        @Extension
        @Symbol("fooBarInner")
        public static class DescriptorImpl extends Descriptor<FooBar> {

        }
    }
}
