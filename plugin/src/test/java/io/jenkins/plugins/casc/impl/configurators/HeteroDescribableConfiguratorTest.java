package io.jenkins.plugins.casc.impl.configurators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import hudson.markup.RawHtmlMarkupFormatter;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.security.SecurityRealm;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.UnknownAttributesException;
import java.util.Objects;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.stapler.DataBoundConstructor;

public class HeteroDescribableConfiguratorTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public EnvironmentVariables environment = new EnvironmentVariables();

    @Test
    public void testScalarVariableInterpolation() {
        environment.set("ENV_FORMATTER", "rawHtml");

        String yamlResource = Objects.requireNonNull(
                        getClass().getResource("HeteroDescribableConfiguratorTest_scalarInterpolation.yml"))
                .toExternalForm();
        ConfigurationAsCode.get().configure(yamlResource);

        assertTrue(
                "Markup formatter should be resolved to RawHtmlMarkupFormatter",
                Jenkins.get().getMarkupFormatter() instanceof RawHtmlMarkupFormatter);
    }

    @Test
    public void testInvalidResolvedVariableThrowsException() {
        environment.set("ENV_FORMATTER", "some-invalid-formatter-name");

        String yamlResource = Objects.requireNonNull(
                        getClass().getResource("HeteroDescribableConfiguratorTest_scalarInterpolation.yml"))
                .toExternalForm();

        UnknownAttributesException thrown =
                assertThrows(UnknownAttributesException.class, () -> ConfigurationAsCode.get()
                        .configure(yamlResource));

        assertThat(
                "Exception should mention the failure to find an implementation",
                thrown.getMessage(),
                containsString(
                        "No hudson.markup.MarkupFormatter implementation found for some-invalid-formatter-name"));
    }

    @Test
    public void shouldFailWhenMultipleImplementationsProvidedForNestedSingleValuedDescribable() {
        ConfiguratorException exception = assertThrows(ConfiguratorException.class, () -> ConfigurationAsCode.get()
                .configure(Objects.requireNonNull(getClass().getResource("multiple-implementations.yml"))
                        .toExternalForm()));

        String errorMessage = exception.getMessage();
        if (exception.getCause() != null) {
            errorMessage += " " + exception.getCause().getMessage();
        }

        assertThat(errorMessage, containsString("found multiple entries"));
        assertThat(errorMessage, containsString("wellKnown"));
        assertThat(errorMessage, containsString("manual"));
    }

    public static class DummySecurityRealm extends SecurityRealm {
        private final DummyServerConfiguration serverConfiguration;

        @DataBoundConstructor
        public DummySecurityRealm(DummyServerConfiguration serverConfiguration) {
            this.serverConfiguration = serverConfiguration;
        }

        @SuppressWarnings("unused")
        public DummyServerConfiguration getServerConfiguration() {
            return serverConfiguration;
        }

        @Override
        public SecurityComponents createSecurityComponents() {
            return new SecurityComponents();
        }

        @TestExtension
        @Symbol("dummyOic")
        public static class DescriptorImpl extends Descriptor<SecurityRealm> {}
    }

    public abstract static class DummyServerConfiguration implements Describable<DummyServerConfiguration> {
        @SuppressWarnings("unchecked")
        @Override
        public Descriptor<DummyServerConfiguration> getDescriptor() {
            return Jenkins.get().getDescriptorOrDie(getClass());
        }
    }

    public static class WellKnownConfig extends DummyServerConfiguration {
        @DataBoundConstructor
        public WellKnownConfig() {}

        @TestExtension
        @Symbol("wellKnown")
        public static class DescriptorImpl extends Descriptor<DummyServerConfiguration> {}
    }

    public static class ManualConfig extends DummyServerConfiguration {
        @DataBoundConstructor
        public ManualConfig() {}

        @TestExtension
        @Symbol("manual")
        public static class DescriptorImpl extends Descriptor<DummyServerConfiguration> {}
    }
}
