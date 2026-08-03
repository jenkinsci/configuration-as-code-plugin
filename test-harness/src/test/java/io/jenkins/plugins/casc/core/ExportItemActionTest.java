package io.jenkins.plugins.casc.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import hudson.model.FreeStyleProject;
import org.htmlunit.html.HtmlPage;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ExportItemActionTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testActionMetadataAndGetConfig() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("test-export-job");
        ExportItemAction action = new ExportItemAction(project);

        assertEquals("Export", action.getDisplayName());
        assertEquals("jcasc-export", action.getUrlName());
        assertEquals(project, action.getItem());

        String yamlConfig = action.getConfig();

        assertNotNull("YAML configuration string should not be null", yamlConfig);
        assertTrue("YAML should contain items root", yamlConfig.contains("items:"));
        assertTrue("YAML should contain freestyle type", yamlConfig.contains("- freestyle:"));
        assertTrue("YAML should contain the job name", yamlConfig.contains("name: \"test-export-job\""));
    }

    @Test
    public void testExportUiPageRendersYaml() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("test-job");

        JenkinsRule.WebClient client = j.createWebClient();

        HtmlPage page = client.goTo(project.getUrl() + "jcasc-export/");

        String pageText = page.asNormalizedText();

        assertTrue("Page should contain the items root", pageText.contains("items:"));
        assertTrue("Page should contain the freestyle configurator type", pageText.contains("- freestyle:"));
        assertTrue("Page should contain the job name", pageText.contains("name: \"test-job\""));
    }
}
