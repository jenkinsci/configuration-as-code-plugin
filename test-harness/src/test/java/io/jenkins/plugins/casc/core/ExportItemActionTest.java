package io.jenkins.plugins.casc.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import hudson.model.FreeStyleProject;
import java.net.URL;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
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

        assertEquals("Export Configuration", action.getDisplayName());
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

    @Test
    public void testDownloadYamlEndpoint() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("test-job");
        JenkinsRule.WebClient client = j.createWebClient();

        WebRequest request =
                new WebRequest(new URL(j.getURL() + project.getUrl() + "jcasc-export/downloadYaml"), HttpMethod.POST);

        client.addCrumb(request);

        WebResponse response = client.getPage(request).getWebResponse();

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getContentAsString().contains("name: \"test-job\""));
    }
}
