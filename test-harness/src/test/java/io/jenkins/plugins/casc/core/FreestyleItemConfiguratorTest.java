package io.jenkins.plugins.casc.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BatchFile;
import hudson.tasks.LogRotator;
import hudson.tasks.Shell;
import hudson.triggers.TimerTrigger;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;

public class FreestyleItemConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("freestyle-job.yaml")
    public void shouldCreateFreestyleJob() {
        FreeStyleProject job = (FreeStyleProject) Jenkins.get().getItem("my-freestyle-job");

        assertNotNull("Freestyle job should have been created by JCasC", job);
        assertEquals("My Freestyle Job", job.getDisplayName());
        assertEquals("Configured via JCasC", job.getDescription());
    }

    @Test
    public void shouldUpdateExistingFreestyleJob() throws Exception {
        FreeStyleProject existingJob = j.jenkins.createProject(FreeStyleProject.class, "existing-freestyle-job");
        existingJob.setDescription("Old Description");
        existingJob.setDisplayName("Old Display Name");

        ConfigurationAsCode.get()
                .configure(Objects.requireNonNull(getClass().getResource("update-freestyle-job.yaml"))
                        .toExternalForm());

        FreeStyleProject updatedJob = (FreeStyleProject) Jenkins.get().getItem("existing-freestyle-job");
        assertNotNull(updatedJob);
        assertEquals("Updated Freestyle Job", updatedJob.getDisplayName());
        assertEquals("Updated via JCasC", updatedJob.getDescription());
    }

    @Test
    @ConfiguredWithCode("freestyle-job-full.yaml")
    public void shouldConfigureMajorityOfJobProperties() {
        FreeStyleProject job = (FreeStyleProject) Jenkins.get().getItem("my-freestyle-full-job");

        assertNotNull("Freestyle job should have been created by JCasC", job);

        assertEquals("My Freestyle Job", job.getDisplayName());
        assertEquals("Configured via JCasC", job.getDescription());
        assertTrue("Concurrent builds should be enabled", job.isConcurrentBuild());
        assertEquals("Quiet period should be updated", 50, job.getQuietPeriod());

        assertNotNull("Build discarder should be configured", job.getBuildDiscarder());
        assertTrue("Build discarder should be a LogRotator", job.getBuildDiscarder() instanceof LogRotator);
        LogRotator logRotator = (LogRotator) job.getBuildDiscarder();
        assertEquals("Should keep for 7 days", 7, logRotator.getDaysToKeep());
        assertEquals("Should keep 10 builds", 10, logRotator.getNumToKeep());
        assertEquals(2, job.getBuildersList().size());

        assertTrue("First builder should be a Shell step", job.getBuildersList().get(0) instanceof Shell);
        Shell shell = (Shell) job.getBuildersList().get(0);
        assertEquals("echo Hello", shell.getCommand());

        assertTrue(
                "Second builder should be a BatchFile step",
                job.getBuildersList().get(1) instanceof BatchFile);
        BatchFile batch = (BatchFile) job.getBuildersList().get(1);
        assertEquals("echo Windows", batch.getCommand());

        assertEquals(1, job.getPublishersList().size());
        assertTrue(
                "Publisher should be ArtifactArchiver", job.getPublishersList().get(0) instanceof ArtifactArchiver);
        ArtifactArchiver archiver = (ArtifactArchiver) job.getPublishersList().get(0);
        assertEquals("*.jar", archiver.getArtifacts());

        assertEquals(1, job.getTriggers().size());
        assertTrue(
                "Trigger should be a TimerTrigger (cron)",
                job.getTriggers().values().iterator().next() instanceof TimerTrigger);
        TimerTrigger trigger =
                (TimerTrigger) job.getTriggers().values().iterator().next();
        assertEquals("* * * * *", trigger.getSpec());

        ParametersDefinitionProperty paramProp = job.getProperty(ParametersDefinitionProperty.class);
        assertNotNull("Job should have parameter definitions", paramProp);
        assertEquals(1, paramProp.getParameterDefinitions().size());
        assertTrue(
                "Parameter should be a StringParameter",
                paramProp.getParameterDefinitions().get(0) instanceof StringParameterDefinition);

        StringParameterDefinition stringParam =
                (StringParameterDefinition) paramProp.getParameterDefinitions().get(0);
        assertEquals("DEPLOY_ENV", stringParam.getName());
        assertEquals("dev", stringParam.getDefaultValue());
    }

    @Test
    @ConfiguredWithCode("freestyle-job-full.yaml")
    public void shouldBeIdempotent() {
        FreeStyleProject job = (FreeStyleProject) Jenkins.get().getItem("my-freestyle-full-job");
        assertNotNull(job);

        ConfigurationAsCode.get()
                .configure(Objects.requireNonNull(getClass().getResource("freestyle-job-full.yaml"))
                        .toExternalForm());

        assertEquals("Builders should not duplicate", 2, job.getBuildersList().size());
        assertEquals(
                "Publishers should not duplicate", 1, job.getPublishersList().size());
        assertEquals("Triggers should not duplicate", 1, job.getTriggers().size());

        ParametersDefinitionProperty paramProp = job.getProperty(ParametersDefinitionProperty.class);
        assertNotNull("Properties should exist", paramProp);
        assertEquals(
                "Properties should not duplicate",
                1,
                paramProp.getParameterDefinitions().size());
    }

    @Test
    public void shouldDescribeAttributesCorrectly() throws Exception {
        FreestyleItemConfigurator configurator = new FreestyleItemConfigurator();
        Set<Attribute<FreeStyleProject, ?>> attributes = configurator.describe();

        boolean foundBuildWrappers = attributes.stream().anyMatch(a -> "buildWrappers".equals(a.getName()));
        assertTrue("Should have renamed buildWrappersList to buildWrappers", foundBuildWrappers);

        boolean foundDisplayNameOrNull = attributes.stream().anyMatch(a -> "displayNameOrNull".equals(a.getName()));
        assertFalse("Should have removed displayNameOrNull", foundDisplayNameOrNull);

        Attribute<FreeStyleProject, ?> nameAttr = attributes.stream()
                .filter(a -> "name".equals(a.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull("Should have added 'name' attribute", nameAttr);

        FreeStyleProject dummyProject = j.jenkins.createProject(FreeStyleProject.class, "dummy-project");
        Object nameValue = nameAttr.getValue(dummyProject);
        assertEquals("dummy-project", nameValue);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldThrowExceptionOnInstance() {
        new FreestyleItemConfigurator().instance(null, null);
    }

    @Test
    public void shouldCatchAndWrapGeneralExceptionInConfigure() {
        FreestyleItemConfigurator configurator = new FreestyleItemConfigurator();
        try {
            configurator.configure("error-job", null, null);
            fail("Should have thrown ConfiguratorException");
        } catch (ConfiguratorException e) {
            assertTrue(e.getMessage().contains("Failed to configure freestyle job: error-job"));
        }
    }

    @Test
    public void shouldRethrowConfiguratorExceptionInConfigure() throws Exception {
        String yaml = "items:\n  - freestyle:\n      name: bad-job\n      unknownProperty: 123";
        Path tempFile = Files.createTempFile("bad-job", ".yaml");
        tempFile.toFile().deleteOnExit();
        Files.writeString(tempFile, yaml);

        try {
            ConfigurationAsCode.get().configure(tempFile.toUri().toString());
            fail("Should have thrown ConfiguratorException for unknown property");
        } catch (ConfiguratorException e) {
            assertTrue(e.getMessage() != null || e.getCause() instanceof ConfiguratorException);
        }
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void shouldHandleExceptionsInSetters() throws Exception {
        FreestyleItemConfigurator configurator = new FreestyleItemConfigurator();
        Attribute propertiesAttr = configurator.describe().stream()
                .filter(a -> "properties".equals(a.getName()))
                .findFirst()
                .orElse(null);
        Attribute triggersAttr = configurator.describe().stream()
                .filter(a -> "triggers".equals(a.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(propertiesAttr);
        assertNotNull(triggersAttr);

        FreeStyleProject job = j.jenkins.createProject(FreeStyleProject.class, "fail-job");

        try {
            propertiesAttr.setValue(job, Collections.singletonList(null));
            fail("Expected IllegalStateException for properties setter");
        } catch (IllegalStateException e) {
            assertEquals("Failed to apply properties", e.getMessage());
        }

        try {
            triggersAttr.setValue(job, Collections.singletonList(null));
            fail("Expected IllegalStateException for triggers setter");
        } catch (IllegalStateException e) {
            assertEquals("Failed to apply triggers", e.getMessage());
        }
    }

    @Test
    public void shouldReturnCorrectNameAndTarget() {
        FreestyleItemConfigurator configurator = new FreestyleItemConfigurator();
        assertEquals("freestyle", configurator.getName());
        assertEquals(FreeStyleProject.class, configurator.getTarget());
    }
}
