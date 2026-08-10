package io.jenkins.plugins.casc.core;

import static io.jenkins.plugins.casc.ConfigurationAsCode.get;
import static java.util.Collections.emptyList;
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
import hudson.model.AbstractItem;
import hudson.model.FreeStyleProject;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.ItemConfigurator;
import io.jenkins.plugins.casc.core.CascItemProperty.DescriptorImpl;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;
import io.jenkins.plugins.casc.model.Sequence;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.TestExtension;

public class ItemsRootConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    private Mapping dummyJob(String name) {
        Mapping properties = new Mapping();
        properties.put("name", new Scalar(name));
        Mapping item = new Mapping();
        item.put("dummy", properties);
        return item;
    }

    private Mapping root(String strategy, String... jobNames) {
        Sequence itemsSequence = new Sequence();
        for (String name : jobNames) {
            itemsSequence.add(dummyJob(name));
        }
        Mapping root = new Mapping();
        if (strategy != null) {
            root.put("actionOnUndeclaredItems", new Scalar(strategy));
        }
        root.put("items", itemsSequence);
        return root;
    }

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

    @Test
    public void shouldSyncRemoveManagedNonJobItem() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        Mapping properties = new Mapping();
        properties.put("name", new Scalar("my-non-job"));
        Mapping item = new Mapping();
        item.put("dummy-non-job", properties);

        Sequence items = new Sequence();
        items.add(item);

        Mapping configRoot = new Mapping();
        configRoot.put("actionOnUndeclaredItems", new Scalar("delete-tracked"));
        configRoot.put("items", items);

        configurator.configure(configRoot, context);

        TopLevelItem createdItem = j.jenkins.getItem("my-non-job");
        assertNotNull("Non-job item should be created", createdItem);
        assertFalse("Item should explicitly not be an instance of Job", createdItem instanceof Job);

        File markerFile = new File(createdItem.getRootDir(), ".casc-managed");
        assertTrue("CasC marker file should exist in the root dir", markerFile.exists());

        configurator.configure(root("delete-tracked"), context);

        assertNull(
                "Non-job item should be deleted during sync because it was CasC managed",
                j.jenkins.getItem("my-non-job"));
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

    public static class DummyNonJob extends AbstractItem implements TopLevelItem {
        public DummyNonJob(ItemGroup parent, String name) {
            super(parent, name);
        }

        @Override
        public Collection<? extends Job<?, ?>> getAllJobs() {
            return emptyList();
        }

        @Override
        public TopLevelItemDescriptor getDescriptor() {
            return (TopLevelItemDescriptor) Jenkins.get().getDescriptorOrDie(getClass());
        }
    }

    @TestExtension
    @SuppressWarnings("unused")
    public static class DummyNonJobDescriptor extends TopLevelItemDescriptor {
        public DummyNonJobDescriptor() {
            super(DummyNonJob.class);
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return "Dummy Non Job";
        }

        @Override
        public TopLevelItem newInstance(ItemGroup parent, String name) {
            return new DummyNonJob(parent, name);
        }
    }

    @TestExtension
    @SuppressWarnings("unused")
    public static class DummyNonJobConfigurator implements ItemConfigurator<DummyNonJob> {

        @Override
        public @NonNull String getName() {
            return "dummy-non-job";
        }

        @Override
        public Class<DummyNonJob> getTarget() {
            return DummyNonJob.class;
        }

        @Override
        public DummyNonJob configure(String name, CNode config, ConfigurationContext context)
                throws ConfiguratorException {
            try {
                Jenkins jenkins = Jenkins.get();
                TopLevelItem item = jenkins.getItem(name);
                if (item == null) {
                    item = jenkins.createProject(DummyNonJob.class, name);
                }
                item.save();
                return (DummyNonJob) item;
            } catch (IOException e) {
                throw new ConfiguratorException("Failed to configure dummy non-job: " + name, e);
            }
        }

        @Override
        public CNode describe(DummyNonJob instance, ConfigurationContext context) {
            return null;
        }

        @Override
        @NonNull
        public Set<Attribute<DummyNonJob, ?>> describe() {
            return emptySet();
        }

        @Override
        @NonNull
        public DummyNonJob configure(CNode config, ConfigurationContext context) {
            throw new UnsupportedOperationException("Requires a name to configure");
        }

        @Override
        public DummyNonJob check(CNode config, ConfigurationContext context) {
            return null;
        }
    }

    public static class ScopedSystemProperty implements AutoCloseable {
        private final String key;
        private final String previousValue;

        public ScopedSystemProperty(String key, String value) {
            this.key = key;
            this.previousValue = System.getProperty(key);
            System.setProperty(key, value);
        }

        @Override
        public void close() {
            if (previousValue == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, previousValue);
            }
        }
    }

    @Test
    public void shouldReturnTargetComponent() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        assertEquals(configurator, configurator.getTargetComponent(null));
    }

    @Test
    public void shouldReturnNullOnDescribe() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        assertNull(configurator.describe(configurator, null));
    }

    @Test
    public void shouldBeDiscoveredForReferenceDocumentation() {
        Collection<?> configurators = get().getConfigurators();

        assertTrue(
                "ItemsRootConfigurator should be included in the reference documentation model",
                configurators.stream().anyMatch(ItemsRootConfigurator.class::isInstance));
        assertTrue(
                "Freestyle item configurator should be reachable from the items root",
                configurators.stream().anyMatch(FreestyleItemConfigurator.class::isInstance));
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

        Sequence itemsSequence = new Sequence();
        itemsSequence.add(dummyJob("my-check-job"));

        ItemsRootConfigurator result = configurator.check(itemsSequence, context);

        assertNotNull("Check should return a non-null ItemsRootConfigurator instance", result);
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

    @Test
    public void shouldNotRemoveUnmanagedItemsWhenStrategyIsNone() throws Exception {
        j.createFreeStyleProject("manual-job");

        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        configurator.configure(root(null, "casc-job"), context);

        assertNotNull("Manual job should be untouched by default/none strategy", j.jenkins.getItem("manual-job"));
        assertNotNull("CasC job should be created", j.jenkins.getItem("casc-job"));
    }

    @Test
    public void shouldRemoveAllUnconfiguredItemsWhenStrategyIsRemoveAll() throws Exception {
        j.createFreeStyleProject("manual-job");

        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        configurator.configure(root("delete-all", "casc-job"), context);

        assertNull("Manual job should be deleted by remove-all strategy", j.jenkins.getItem("manual-job"));
        assertNotNull("CasC job should exist", j.jenkins.getItem("casc-job"));
    }

    @Test
    public void shouldOnlyRemoveCascManagedItemsWhenStrategyIsSync() throws Exception {
        j.createFreeStyleProject("manual-job");

        FreeStyleProject oldCascJob = j.createFreeStyleProject("old-casc-job");
        oldCascJob.addProperty(new CascItemProperty());
        oldCascJob.save();

        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        configurator.configure(root("delete-tracked", "new-casc-job"), context);

        assertNotNull("Manual job should be untouched by sync strategy", j.jenkins.getItem("manual-job"));
        assertNull("Old CasC job should be deleted because it is no longer in YAML", j.jenkins.getItem("old-casc-job"));
        assertNotNull("New CasC job should exist", j.jenkins.getItem("new-casc-job"));

        FreeStyleProject newJob = (FreeStyleProject) j.jenkins.getItem("new-casc-job");
        File cascMarker = new File(requireNonNull(newJob).getRootDir(), ".casc-managed");
        assertTrue("New job must have the .casc-managed marker file", cascMarker.exists());
    }

    @Test
    public void shouldNotDeleteMultipleUnmanagedJobsWhenStrategyIsSync() throws Exception {
        j.createFreeStyleProject("manual-job-1");
        j.createFreeStyleProject("manual-job-2");

        FreeStyleProject oldCascJob = j.createFreeStyleProject("old-casc-job");
        oldCascJob.addProperty(new CascItemProperty());
        oldCascJob.save();

        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        configurator.configure(root("delete-tracked", "new-casc-job"), context);

        assertNotNull("manual-job-1 should be untouched", j.jenkins.getItem("manual-job-1"));
        assertNotNull("manual-job-2 should be untouched", j.jenkins.getItem("manual-job-2"));
        assertNull("old-casc-job should be deleted", j.jenkins.getItem("old-casc-job"));
        assertNotNull("new-casc-job should be created", j.jenkins.getItem("new-casc-job"));
    }

    @Test
    public void shouldKeepAndConfigureManagedJobWhenStillPresent() throws Exception {
        FreeStyleProject oldCascJob = j.createFreeStyleProject("old-casc-job");
        oldCascJob.addProperty(new CascItemProperty());
        oldCascJob.setDescription("original description");
        oldCascJob.save();

        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        Mapping properties = new Mapping();
        properties.put("name", new Scalar("old-casc-job"));
        properties.put("description", new Scalar("updated description"));
        Mapping item = new Mapping();
        item.put("dummy", properties);
        Sequence items = new Sequence();
        items.add(item);

        Mapping root = new Mapping();
        root.put("actionOnUndeclaredItems", new Scalar("delete-tracked"));
        root.put("items", items);

        configurator.configure(root, context);

        assertNotNull("Managed job should still exist", j.jenkins.getItem("old-casc-job"));
        assertEquals(
                "Managed job should be updated rather than recreated",
                "updated description",
                oldCascJob.getDescription());
    }

    @Test
    public void shouldHandlePartialRemovalDuringSync() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        configurator.configure(root("delete-tracked", "job-A", "job-B"), context);
        assertNotNull(j.jenkins.getItem("job-A"));
        assertNotNull(j.jenkins.getItem("job-B"));

        configurator.configure(root("delete-tracked", "job-A"), context);

        assertNotNull("Job A should exist", j.jenkins.getItem("job-A"));
        assertNull("Job B should be removed via partial sync", j.jenkins.getItem("job-B"));
        assertEquals("Total items count should be 1", 1, j.jenkins.getItems().size());
    }

    @Test
    public void shouldRemoveAllCascManagedItemsWhenSyncWithEmptyList() throws Exception {
        j.createFreeStyleProject("manual-job");

        FreeStyleProject oldCascJob = j.createFreeStyleProject("old-casc-job");
        oldCascJob.addProperty(new CascItemProperty());
        oldCascJob.save();

        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        configurator.configure(root("delete-tracked"), context);

        assertNotNull("Manual job should be preserved", j.jenkins.getItem("manual-job"));
        assertNull("Old CasC job should be deleted because items list is empty", j.jenkins.getItem("old-casc-job"));
    }

    @Test
    public void shouldFailOnInvalidActionOnUndeclaredItems() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        ConfiguratorException e =
                assertThrows(ConfiguratorException.class, () -> configurator.configure(root("abc", "job-A"), context));

        assertTrue(
                "Message did not match. Got: " + e.getMessage(),
                e.getMessage().contains("Invalid actionOnUndeclaredItems: abc"));
    }

    @Test
    public void shouldInterpolateVariablesInItemName() {
        try (ScopedSystemProperty ignored = new ScopedSystemProperty("MY_INTERPOLATED_JOB_NAME", "dynamic-job-name")) {
            ItemsRootConfigurator configurator = new ItemsRootConfigurator();
            ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

            configurator.configure(root(null, "${MY_INTERPOLATED_JOB_NAME}"), context);

            assertNotNull("Job should be created using the interpolated name", j.jenkins.getItem("dynamic-job-name"));
        }
    }

    @Test
    public void shouldBeIdempotentWhenApplyingSameConfigTwice() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        Sequence itemsSequence = new Sequence();
        itemsSequence.add(dummyJob("idempotent-job"));

        configurator.configure(itemsSequence, context);
        int itemCountAfterFirstApply = j.jenkins.getItems().size();
        assertEquals(1, itemCountAfterFirstApply);
        assertNotNull(j.jenkins.getItem("idempotent-job"));

        Sequence itemsSequence2 = new Sequence();
        itemsSequence2.add(dummyJob("idempotent-job"));

        configurator.configure(itemsSequence2, context);
        int itemCountAfterSecondApply = j.jenkins.getItems().size();

        assertEquals(
                "Applying the same config twice should not duplicate or change item count",
                itemCountAfterFirstApply,
                itemCountAfterSecondApply);
    }

    @Test
    public void shouldHandleRepeatedSyncConfigurations() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        configurator.configure(root("delete-tracked", "job-A"), context);
        assertNotNull("Job A should exist after first sync", j.jenkins.getItem("job-A"));
        assertEquals(1, j.jenkins.getItems().size());

        configurator.configure(root("delete-tracked", "job-B"), context);

        assertNull(
                "Job A should be deleted by the second sync because it is missing from YAML",
                j.jenkins.getItem("job-A"));
        assertNotNull("Job B should be created by the second sync", j.jenkins.getItem("job-B"));
        assertEquals("Total items should remain 1", 1, j.jenkins.getItems().size());
    }

    @Test
    public void shouldHandleMissingItemsKey() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        Mapping root = new Mapping();
        root.put("actionOnUndeclaredItems", new Scalar("delete-tracked"));

        configurator.configure(root, context);

        assertEquals(0, j.jenkins.getItems().size());
    }

    @Test
    public void shouldCheckValidMappingConfiguration() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        ItemsRootConfigurator result = configurator.check(root("delete-tracked", "job-A"), context);

        assertNotNull(result);
    }

    @Test
    public void shouldFailOnInvalidItemsMapping() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        Mapping mapping = new Mapping();
        mapping.put("foo", new Scalar("bar"));

        ConfiguratorException e = assertThrows(ConfiguratorException.class, () -> configurator.check(mapping, context));

        assertTrue(e.getMessage().contains("Invalid items configuration"));
    }

    @Test
    public void shouldFailWhenItemsIsNotSequence() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        Mapping root = new Mapping();
        root.put("items", new Mapping());

        ConfiguratorException e = assertThrows(ConfiguratorException.class, () -> configurator.check(root, context));

        assertTrue(e.getMessage().contains("Expected a sequence of items"));
    }

    @Test
    public void shouldNotRetagAlreadyManagedJob() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("job-A");
        job.addProperty(new CascItemProperty());
        job.save();

        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        configurator.configure(root(null, "job-A"), context);

        assertNotNull(job.getProperty(CascItemProperty.class));
    }

    @Test
    public void shouldNotDeleteConfiguredItemWithRemoveAll() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        configurator.configure(root("delete-all", "job-A"), context);

        assertNotNull(j.jenkins.getItem("job-A"));
    }

    @Test
    public void shouldFailCheckOnInvalidActionOnUndeclaredItems() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        Mapping root = new Mapping();
        root.put("actionOnUndeclaredItems", new Scalar("invalid"));
        root.put("items", new Sequence());

        assertThrows(ConfiguratorException.class, () -> configurator.check(root, context));
    }

    @Test
    public void shouldFailOnEmptyMapping() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        Mapping mapping = new Mapping();

        assertThrows(ConfiguratorException.class, () -> configurator.check(mapping, context));
    }

    @Test
    public void shouldAllowMissingItemsWhenOnlyStrategySpecified() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        Mapping root = new Mapping();
        root.put("actionOnUndeclaredItems", new Scalar("keep"));

        configurator.configure(root, context);

        assertEquals(0, j.jenkins.getItems().size());
    }

    @Test
    public void testCascItemPropertyAndDescriptor() {
        CascItemProperty property = new CascItemProperty();
        assertNotNull("Property instance should be created", property);

        DescriptorImpl descriptor = new DescriptorImpl();
        assertTrue("Descriptor should be applicable to any Job type", descriptor.isApplicable(FreeStyleProject.class));
        assertNotNull("Display name should not be null", descriptor.getDisplayName());
    }

    @Test
    public void shouldRejectUnknownKeyBeforeApplyingRemoveAll() throws IOException {
        j.createFreeStyleProject("manual-job");

        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        Mapping root = new Mapping();
        root.put("actionOnUndeclaredItems", new Scalar("remove-all"));
        root.put("itmes", new Sequence());

        ConfiguratorException e =
                assertThrows(ConfiguratorException.class, () -> configurator.configure(root, context));

        assertTrue(e.getMessage().contains("Unsupported key 'itmes'"));
        assertNotNull("Item must not be deleted when configuration validation fails", j.jenkins.getItem("manual-job"));
    }

    @Test
    public void shouldRejectUnknownKeyDuringSync() throws IOException {
        j.createFreeStyleProject("manual-job");

        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        Mapping root = new Mapping();
        root.put("actionOnUndeclaredItems", new Scalar("sync"));
        root.put("itmes", new Sequence());

        ConfiguratorException e =
                assertThrows(ConfiguratorException.class, () -> configurator.configure(root, context));

        assertTrue(e.getMessage().contains("Unsupported key 'itmes'"));
        assertNotNull(j.jenkins.getItem("manual-job"));
    }

    @Test
    public void shouldRejectUnknownKeyEvenWhenItemsArePresent() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        Mapping root = root("sync", "job-A");
        root.put("itmes", new Sequence());

        ConfiguratorException e = assertThrows(ConfiguratorException.class, () -> configurator.check(root, context));

        assertTrue(e.getMessage().contains("Unsupported key 'itmes'"));
    }

    @Test
    public void shouldCreateCascMarkerForJob() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        configurator.configure(root("keep", "job-A"), context);

        TopLevelItem item = j.jenkins.getItem("job-A");
        assertNotNull(item);

        File markerFile = new File(item.getRootDir(), ".casc-managed");
        assertTrue("CasC marker should exist for Job", markerFile.exists());
    }

    @Test
    public void shouldDeleteItemDirectoryDuringSync() {
        ItemsRootConfigurator configurator = new ItemsRootConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        configurator.configure(root("delete-tracked", "job-A"), context);

        TopLevelItem item = j.jenkins.getItem("job-A");
        assertNotNull(item);

        File rootDir = item.getRootDir();
        assertTrue(rootDir.exists());

        configurator.configure(root("delete-tracked"), context);

        assertNull(j.jenkins.getItem("job-A"));
        assertFalse("Item directory should be deleted", rootDir.exists());
    }
}
