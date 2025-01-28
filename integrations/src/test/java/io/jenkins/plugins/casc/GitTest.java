package io.jenkins.plugins.casc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.plugins.git.GitSCM;
import hudson.plugins.git.browser.AssemblaWeb;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries;
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration;
import org.jenkinsci.plugins.workflow.libs.SCMRetriever;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.LogRecorder;

/**
 * Tests for Git plugin global configurations.
 */
@WithJenkinsConfiguredWithCode
class GitTest {

    private final LogRecorder logging = new LogRecorder()
            .record(Logger.getLogger(Attribute.class.getName()), Level.INFO)
            .capture(2048);

    @AfterEach
    void dumpLogs() {
        for (String message : logging.getMessages()) {
            System.out.println(message);
        }
    }

    @Test
    @Issue("JENKINS-57604")
    @ConfiguredWithCode("GitTest.yml")
    void checkAssemblaWebIsLoaded(JenkinsConfiguredWithCodeRule j) {
        final Jenkins jenkins = Jenkins.get();
        final GlobalLibraries libs =
                jenkins.getExtensionList(GlobalConfiguration.class).get(GlobalLibraries.class);

        LibraryConfiguration lib = libs.getLibraries().get(0);
        SCMRetriever retriever = (SCMRetriever) lib.getRetriever();
        GitSCM scm = (GitSCM) retriever.getScm();
        AssemblaWeb browser = (AssemblaWeb) scm.getBrowser();
        assertEquals("assembla.acmecorp.com", browser.getRepoUrl());
    }
}
