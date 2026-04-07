package io.jenkins.plugins.casc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import jenkins.model.Jenkins;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.HttpMethod;
import org.htmlunit.Page;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

public class ConfigurationAsCodeLastAppliedTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private static final String ENDPOINT = "configuration-as-code/configure";
    private static final String DOWNLOAD_ENDPOINT = "configuration-as-code/downloadLastAppliedConfiguration";
    private static final String YAML_CONTENT_TYPE = "application/yaml";
    private static final String ADMIN = "admin";

    private WebRequest webRequest(String endpoint) throws Exception {
        return new WebRequest(new URL(j.getURL(), endpoint), HttpMethod.POST);
    }

    private WebRequest yamlPost(String requestBody) throws Exception {
        WebRequest request = webRequest(ENDPOINT);
        request.setAdditionalHeader("Content-Type", YAML_CONTENT_TYPE);
        if (requestBody != null) {
            request.setRequestBody(requestBody);
        }
        return request;
    }

    private WebRequest post() throws Exception {
        return webRequest(ConfigurationAsCodeLastAppliedTest.DOWNLOAD_ENDPOINT);
    }

    private void configureAdminSecurity() {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER, Jenkins.SYSTEM_READ)
                .everywhere()
                .to(ADMIN));
        j.jenkins.setCrumbIssuer(null);
    }

    private Path lastAppliedFile() {
        return j.jenkins.getRootDir().toPath().resolve(ConfigurationAsCode.LAST_APPLIED_CONFIG_FILE);
    }

    @Test
    public void testSuccessfulApplySavesFile() throws Exception {
        configureAdminSecurity();
        String validYaml = "jenkins:\n  systemMessage: 'Valid Message'\n";

        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            wc.login(ADMIN);
            wc.getPage(yamlPost(validYaml));

            assertTrue("Last applied config file should exist", Files.exists(lastAppliedFile()));
            assertTrue("UI helper should return true", ConfigurationAsCode.get().isLastAppliedConfigurationAvailable());

            String content = Files.readString(lastAppliedFile());
            assertTrue(
                    "File should contain the applied configuration",
                    content.contains("systemMessage: \"Valid Message\""));
        }
    }

    @Test
    public void testFailedApplyDoesNotOverwriteFile() throws Exception {
        configureAdminSecurity();
        String validYaml = "jenkins:\n  systemMessage: 'Good State'\n";

        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            wc.login(ADMIN);
            wc.getPage(yamlPost(validYaml));
            assertTrue(Files.exists(lastAppliedFile()));

            String invalidYaml = "unsupported_root_element:\n  foo: 'bar'\n";
            try {
                wc.getPage(yamlPost(invalidYaml));
                fail("Expected a 400 Bad Request exception");
            } catch (FailingHttpStatusCodeException e) {
                assertEquals(400, e.getStatusCode());
            }
        }

        String content = Files.readString(lastAppliedFile());
        assertTrue("Should retain previous good state", content.contains("systemMessage: \"Good State\""));
        assertFalse("Should not contain invalid state", content.contains("unsupported_root_element"));
    }

    @Test
    public void testDownloadEndpointWorksForAdmin() throws Exception {
        configureAdminSecurity();

        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            wc.login(ADMIN);
            wc.getPage(yamlPost("jenkins:\n  systemMessage: 'Download Me'\n"));

            Page page = wc.getPage(post());
            WebResponse response = page.getWebResponse();

            assertEquals(200, response.getStatusCode());
            assertEquals("application/x-yaml", response.getContentType());
            assertTrue(response.getResponseHeaderValue("Content-Disposition")
                    .contains(ConfigurationAsCode.LAST_APPLIED_CONFIG_FILE));
            assertTrue(response.getContentAsString().contains("systemMessage: \"Download Me\""));
        }
    }

    @Test
    public void testDownloadEndpointRequiresPermissions() throws Exception {
        configureAdminSecurity();

        try (JenkinsRule.WebClient adminWc = j.createWebClient();
                JenkinsRule.WebClient anonWc = j.createWebClient()) {

            adminWc.login(ADMIN);
            adminWc.getPage(yamlPost("jenkins:\n  systemMessage: 'Secret'\n"));

            try {
                anonWc.getPage(post());
                fail("Expected a 403 Forbidden exception for anonymous user");
            } catch (FailingHttpStatusCodeException e) {
                assertEquals("Should reject unauthorized access", 403, e.getStatusCode());
            }
        }
    }

    @Before
    public void cleanup() throws Exception {
        Files.deleteIfExists(lastAppliedFile());
    }
}
