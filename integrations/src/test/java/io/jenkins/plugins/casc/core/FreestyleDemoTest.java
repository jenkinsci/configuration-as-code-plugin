package io.jenkins.plugins.casc.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BatchFile;
import hudson.tasks.LogRotator;
import hudson.tasks.Shell;
import hudson.triggers.TimerTrigger;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;

public class FreestyleDemoTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("jobs/README.md#0")
    public void shouldConfigureFreestyleJobFromReadme() {
        FreeStyleProject job = (FreeStyleProject) Jenkins.get().getItem("my-freestyle-full-job");

        assertNotNull("Freestyle job should have been created by JCasC from README", job);
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
    @ConfiguredWithReadme("jobs/README.md#1")
    public void shouldConfigureFreestyleJobWithSyncStrategyFromReadme() {
        FreeStyleProject job = (FreeStyleProject) Jenkins.get().getItem("my-freestyle-job");

        assertNotNull("Freestyle job with sync strategy should have been created by JCasC from README", job);
        assertEquals("my-freestyle-job", job.getName());
    }
}
