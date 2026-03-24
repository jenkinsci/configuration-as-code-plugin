package io.jenkins.plugins.casc.impl.configurators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import hudson.markup.RawHtmlMarkupFormatter;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.UnknownAttributesException;
import java.util.Objects;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.jvnet.hudson.test.JenkinsRule;

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
}
