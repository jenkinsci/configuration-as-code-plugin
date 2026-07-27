package io.jenkins.plugins.casc.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import hudson.model.FreeStyleProject;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.ItemConfigurator;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import java.io.IOException;
import java.util.Objects;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.TestExtension;

public class ItemsRootConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("ItemsRootConfiguratorTest.yml")
    public void shouldDiscoverAndDelegateToItemConfigurator() {
        FreeStyleProject project = j.jenkins.getItemByFullName("my-dummy-job", FreeStyleProject.class);

        assertNotNull("Job should have been created by the dummy configurator", project);
        assertEquals("Configured by JCasC items root configurator", project.getDescription());
    }

    @Test
    public void shouldUpdateExistingJob() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("my-dummy-job");
        p.setDescription("old description");

        ConfigurationAsCode.get()
                .configure(Objects.requireNonNull(getClass().getResource("ItemsRootConfiguratorTest.yml"))
                        .toExternalForm());

        assertEquals("Configured by JCasC items root configurator", p.getDescription());
    }

    @Test
    public void shouldFailOnUnknownType() {
        try {
            ConfigurationAsCode.get()
                    .configure(Objects.requireNonNull(getClass().getResource("ItemsRootConfiguratorTest_unknown.yml"))
                            .toExternalForm());
            fail("Expected a ConfiguratorException to be thrown, but it succeeded.");
        } catch (ConfiguratorException e) {
            assertTrue(
                    "Message did not match. Got: " + e.getMessage(),
                    e.getMessage().contains("No ItemConfigurator found for type: unknown"));
        }
    }

    @Test
    public void shouldFailOnMissingName() {
        try {
            ConfigurationAsCode.get()
                    .configure(
                            Objects.requireNonNull(getClass().getResource("ItemsRootConfiguratorTest_missingName.yml"))
                                    .toExternalForm());
            fail("Expected a ConfiguratorException to be thrown, but it succeeded.");
        } catch (ConfiguratorException e) {
            assertTrue(
                    "Message did not match. Got: " + e.getMessage(),
                    e.getMessage().contains("missing a 'name' attribute"));
        }
    }

    @Test
    public void shouldFailOnMalformedYamlSequence() {
        try {
            ConfigurationAsCode.get()
                    .configure(Objects.requireNonNull(getClass().getResource("ItemsRootConfiguratorTest_malformed.yml"))
                            .toExternalForm());
            fail("Expected an exception to be thrown due to malformed YAML (mapping instead of sequence).");
        } catch (ConfiguratorException | IllegalStateException e) {
            assertNotNull(e);
        }
    }

    @TestExtension
    @SuppressWarnings("unused")
    public static class DummyItemConfigurator implements ItemConfigurator<FreeStyleProject> {

        @Override
        public String getName() {
            return "dummy";
        }

        @Override
        public Class<FreeStyleProject> getTarget() {
            return FreeStyleProject.class;
        }

        @Override
        public FreeStyleProject configure(String name, CNode config, ConfigurationContext context)
                throws ConfiguratorException {
            try {
                Jenkins jenkins = Jenkins.get();
                FreeStyleProject project = (FreeStyleProject) jenkins.getItem(name);

                if (project == null) {
                    project = jenkins.createProject(FreeStyleProject.class, name);
                }

                CNode descNode = config.asMapping().get("description");
                if (descNode != null) {
                    String desc = config.asMapping().getScalarValue("description");
                    project.setDescription(desc);
                }

                project.save();
                return project;

            } catch (IOException e) {
                throw new ConfiguratorException("Failed to configure dummy job: " + name, e);
            }
        }
    }
}
