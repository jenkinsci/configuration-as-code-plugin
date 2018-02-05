package org.jenkinsci.plugins.casc;

import hudson.model.TopLevelItem;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SeedJobTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void inlineSeedJob() throws Exception {
        ConfigurationAsCode.configure(getClass().getResourceAsStream("SeedJobTest/inlineSeedJob.yml"));
        final Jenkins jenkins = Jenkins.getInstance();
        final TopLevelItem test = jenkins.getItem("configuration-as-code");
        assertNotNull(test);
        assertTrue(test instanceof WorkflowMultiBranchProject);
    }

    @Test
    public void externalFileSeedJob() throws Exception {
        System.setProperty("CASC_JENKINS_CONFIG", tempFolder.getRoot().getAbsolutePath());

        FileUtils.write(tempFolder.newFile("jenkins.yml"), fileContentsFromResources("SeedJobTest/externalFileSeedJob.yml"));
        FileUtils.write(tempFolder.newFile("externalFileSeedJob.groovy"), fileContentsFromResources("SeedJobTest/externalFileSeedJob.groovy"));

        ConfigurationAsCode.configure();
        final Jenkins jenkins = Jenkins.getInstance();
        final TopLevelItem test = jenkins.getItem("configuration-as-code");
        assertNotNull(test);
        assertTrue(test instanceof WorkflowMultiBranchProject);

    }

    private String fileContentsFromResources(String fileName) throws IOException {
        String fileContents = null;

        URL url = getClass().getResource(fileName);
        if (url != null) {
            fileContents = IOUtils.toString(url);
        }

        assertNotNull("No file contents for file " + fileName, fileContents);

        return fileContents;
    }

}
