package io.jenkins.plugins.casc;

import hudson.plugins.git.GitSCM;
import hudson.plugins.git.browser.AssemblaWeb;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries;
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration;
import org.jenkinsci.plugins.workflow.libs.SCMRetriever;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.LoggerRule;

import static org.junit.Assert.assertEquals;

/**
 * Tests for Git plugin global configurations.
 */
public class GitTest {

    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();
    public LoggerRule logging = new LoggerRule();

    @Rule
    public RuleChain chain= RuleChain
            .outerRule(logging.record(Logger.getLogger(Attribute.class.getName()), Level.INFO).capture(2048))
            .around(j);

    @After
    public void dumpLogs() {
        for (String message : logging.getMessages()) {
            System.out.println(message);
        }
    }

    @Test
    @Issue("JENKINS-57604")
    @ConfiguredWithCode("GitTest.yml")
    public void checkAssemblaWebIsLoaded() throws Exception {
        final Jenkins jenkins = Jenkins.get();
        final GlobalLibraries libs =  jenkins.getExtensionList(GlobalConfiguration.class)
                .get(GlobalLibraries.class);

        LibraryConfiguration lib = libs.getLibraries().get(0);
        SCMRetriever retriever = (SCMRetriever) lib.getRetriever();
        GitSCM scm = (GitSCM) retriever.getScm();
        AssemblaWeb browser = (AssemblaWeb)scm.getBrowser();
        assertEquals("assembla.acmecorp.com", browser.getRepoUrl());
    }
}
