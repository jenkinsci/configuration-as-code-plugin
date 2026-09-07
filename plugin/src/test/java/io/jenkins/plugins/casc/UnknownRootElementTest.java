package io.jenkins.plugins.casc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class UnknownRootElementTest {

    @Test
    void oneUnknown(JenkinsRule j) {
        ConfiguratorException ex = assertThrows(
                ConfiguratorException.class,
                () -> ConfigurationAsCode.get()
                        .configure(Objects.requireNonNull(getClass().getResource("unknown1.yml"))
                                .toExternalForm()));

        assertThat(ex.getMessage(), containsString("No configurator found for the following root element: alice"));
    }

    @Test
    void twoUnknown(JenkinsRule j) {
        ConfiguratorException ex = assertThrows(
                ConfiguratorException.class,
                () -> ConfigurationAsCode.get()
                        .configure(Objects.requireNonNull(getClass().getResource("unknown2.yml"))
                                .toExternalForm()));

        assertThat(
                ex.getMessage(), containsString("No configurator found for the following root elements: bob, alice"));
    }

    @Test
    void ignoreKnownAlias(JenkinsRule j) throws Exception {
        ConfigurationAsCode.get().configure(getClass().getResource("known.yml").toExternalForm());
        assertThat(j.jenkins.getSystemMessage(), is("Configured by Configuration as Code plugin"));
    }
}
