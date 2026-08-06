package io.jenkins.plugins.casc.core;

import static io.jenkins.plugins.casc.ConfigurationAsCode.get;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.FreeStyleProject;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.ItemConfigurator;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;
import io.jenkins.plugins.casc.model.Sequence;
import java.io.IOException;
import java.util.Set;
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

        assertNotNull("Job should have been created by the configurator", project);
        assertEquals("Configured by JCasC items root configurator", project.getDescription());
        assertFalse("Concurrent builds should be disabled via property", project.isConcurrentBuild());
        assertEquals(
                "Should have exactly 2 build steps",
                2,
                project.getBuildersList().size());
        assertTrue(
                "First builder should be a Shell step",
                project.getBuildersList().get(0) instanceof hudson.tasks.Shell);
        assertEquals(
                "echo 'Hello from JCasC!'",
                ((hudson.tasks.Shell) project.getBuildersList().get(0)).getCommand());
    }

    @Test
    public void shouldUpdateExistingJob() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("my-dummy-job");
        p.setDescription("old description");

        get().configure(requireNonNull(getClass().getResource("ItemsRootConfiguratorTest.yml"))
                .toExternalForm());

        assertEquals("Configured by JCasC items root configurator", p.getDescription());
    }

    @Test
    public void shouldFailOnUnknownType() {
        try {
            get().configure(requireNonNull(getClass().getResource("ItemsRootConfiguratorTest_unknown.yml"))
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
            get().configure(requireNonNull(getClass().getResource("ItemsRootConfiguratorTest_missingName.yml"))
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
            get().configure(requireNonNull(getClass().getResource("ItemsRootConfiguratorTest_malformed.yml"))
                    .toExternalForm());
            fail("Expected an exception to be thrown due to malformed YAML (mapping instead of sequence).");
        } catch (ConfiguratorException | IllegalStateException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void shouldFailOnEmptyName() {
        try {
            get().configure(requireNonNull(getClass().getResource("ItemsRootConfiguratorTest_emptyName.yml"))
                    .toExternalForm());
            fail("Expected a ConfiguratorException to be thrown for empty item name, but it succeeded.");
        } catch (ConfiguratorException e) {
            assertTrue(
                    "Message did not match. Got: " + e.getMessage(),
                    e.getMessage().contains("must have a non-empty 'name' attribute"));
        }
    }

    @TestExtension
    @SuppressWarnings("unused")
    public static class DummyItemConfigurator implements ItemConfigurator<FreeStyleProject> {

        @Override
        public @NonNull String getName() {
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

                Mapping mapping = config.asMapping();

                CNode descNode = mapping.get("description");
                if (descNode != null) {
                    String desc = mapping.getScalarValue("description");
                    project.setDescription(desc);
                }

                CNode propertiesNode = mapping.get("properties");
                if (propertiesNode != null) {
                    for (CNode propNode : propertiesNode.asSequence()) {
                        Mapping propMapping = propNode.asMapping();

                        if (propMapping.containsKey("disableConcurrentBuilds")) {
                            project.setConcurrentBuild(false);
                        }
                    }
                }

                CNode buildersNode = mapping.get("builders");
                if (buildersNode != null) {
                    project.getBuildersList().clear();
                    for (CNode builderNode : buildersNode.asSequence()) {
                        io.jenkins.plugins.casc.model.Mapping builderMapping = builderNode.asMapping();

                        if (builderMapping.containsKey("shell")) {
                            String command =
                                    builderMapping.get("shell").asMapping().getScalarValue("command");
                            project.getBuildersList().add(new hudson.tasks.Shell(command));
                        }
                    }
                }

                project.save();
                return project;

            } catch (IOException e) {
                throw new ConfiguratorException("Failed to configure dummy job: " + name, e);
            }
        }

        @Override
        public CNode describe(FreeStyleProject instance, ConfigurationContext context) {
            return null;
        }

        @Override
        @NonNull
        public Set<Attribute<FreeStyleProject, ?>> describe() {
            return emptySet();
        }

        @Override
        @NonNull
        public FreeStyleProject configure(CNode config, ConfigurationContext context) throws ConfiguratorException {
            throw new UnsupportedOperationException("DummyItemConfigurator requires a name to configure");
        }

        @Override
        public FreeStyleProject check(CNode config, ConfigurationContext context) throws ConfiguratorException {
            return null;
        }
    }

    @Test
    public void shouldReturnTargetComponent() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        assertEquals(Jenkins.get(), configurator.getTargetComponent(null));
    }

    @Test
    public void shouldReturnNullOnDescribe() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        assertNull(configurator.describe(Jenkins.get(), null));
    }

    @Test
    public void shouldFailOnMultipleKeys() {
        try {
            get().configure(requireNonNull(getClass().getResource("ItemsRootConfiguratorTest_multipleKeys.yml"))
                    .toExternalForm());
            fail("Expected a ConfiguratorException to be thrown, but it succeeded.");
        } catch (ConfiguratorException e) {
            assertTrue(
                    "Message did not match. Got: " + e.getMessage(),
                    e.getMessage().contains("exactly one type key"));
        }
    }

    @Test
    public void shouldCheckValidConfiguration() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        Mapping properties = new Mapping();
        properties.put("name", new Scalar("my-check-job"));

        Mapping item = new Mapping();
        item.put("dummy", properties);

        Sequence itemsSequence = new Sequence();
        itemsSequence.add(item);

        Jenkins result = configurator.check(itemsSequence, context);

        assertNotNull("Check should return a non-null Jenkins instance", result);
    }

    @Test
    public void shouldFailCheckOnUnknownType() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        Mapping properties = new Mapping();
        properties.put("name", new Scalar("my-check-job"));

        Mapping item = new Mapping();
        item.put("unknown_type", properties);

        Sequence itemsSequence = new Sequence();
        itemsSequence.add(item);

        ConfiguratorException e =
                assertThrows(ConfiguratorException.class, () -> configurator.check(itemsSequence, context));

        assertTrue(
                "Message did not match. Got: " + e.getMessage(),
                e.getMessage().contains("No ItemConfigurator found for type: unknown_type"));
    }
}
