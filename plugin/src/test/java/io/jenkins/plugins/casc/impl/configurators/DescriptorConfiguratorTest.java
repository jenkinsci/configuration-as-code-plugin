package io.jenkins.plugins.casc.impl.configurators;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.GlobalConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

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

}
