package io.jenkins.plugins.casc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import jenkins.model.Jenkins;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

public class ConfigurationAsCodeApiTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testDoConfigure_RequiresPost() throws Exception {
        WebClient wc = j.createWebClient();
        wc.setThrowExceptionOnFailingStatusCode(false);

        WebRequest request = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.GET);
        WebResponse response = wc.getPage(request).getWebResponse();

        assertTrue(response.getStatusCode() == 404 || response.getStatusCode() == 405);
    }

    @Test
    public void testDoConfigure_Success() throws Exception {
        WebClient wc = j.createWebClient();
        wc.setThrowExceptionOnFailingStatusCode(false);

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to("admin"));

        WebRequest request = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.POST);
        request.setAdditionalHeader("Content-Type", "application/yaml");
        request.setRequestBody("jenkins:\n  systemMessage: 'Webhook Success'");

        wc.withBasicApiToken("admin");

        WebResponse response = wc.getPage(request).getWebResponse();

        assertEquals(200, response.getStatusCode());
        assertEquals("Webhook Success", j.jenkins.getSystemMessage());
    }

    @Test
    public void testDoConfigure_InvalidYaml() throws Exception {
        WebClient wc = j.createWebClient();
        wc.setThrowExceptionOnFailingStatusCode(false);

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to("admin"));

        WebRequest request = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.POST);

        request.setAdditionalHeader("Content-Type", "application/yaml");
        request.setRequestBody("jenkins:\n  systemMessage: [invalid");

        wc.withBasicApiToken("admin");

        WebResponse response = wc.getPage(request).getWebResponse();

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getContentAsString().contains("message"));
    }

    @Test
    public void testDoConfigure_NonAdminForbidden() throws Exception {
        WebClient wc = j.createWebClient();
        wc.setThrowExceptionOnFailingStatusCode(false);

        WebRequest request = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.POST);
        request.setRequestBody("jenkins:\n  systemMessage: 'fail'");

        wc.withBasicApiToken("user");

        WebResponse response = wc.getPage(request).getWebResponse();

        assertEquals(403, response.getStatusCode());
    }

    @Test
    public void testDoConfigure_Unauthenticated() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ)
                .everywhere()
                .toEveryone());

        WebClient wc = j.createWebClient();
        wc.setThrowExceptionOnFailingStatusCode(false);

        WebRequest request = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.POST);

        request.setRequestBody("jenkins:\n  systemMessage: 'anonymous bypass attempt'");

        WebResponse response = wc.getPage(request).getWebResponse();

        assertEquals(403, response.getStatusCode());
    }

    @Test
    public void testDoConfigure_EmptyBody() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to("admin"));

        WebClient wc = j.createWebClient();

        wc.setThrowExceptionOnFailingStatusCode(false);

        WebRequest request = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.POST);
        request.setAdditionalHeader("Content-Type", "application/yaml");

        wc.withBasicApiToken("admin");

        WebResponse response = wc.getPage(request).getWebResponse();

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getContentAsString().contains("message"));
    }

    @Test
    public void testDoConfigure_MalformedStructure() throws Exception {
        WebClient wc = j.createWebClient();
        wc.setThrowExceptionOnFailingStatusCode(false);

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to("admin"));

        WebRequest request = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.POST);
        request.setAdditionalHeader("Content-Type", "application/yaml");
        request.setRequestBody("jenkins:\n  invalidRoot:\n    foo: bar");

        wc.withBasicApiToken("admin");

        WebResponse response = wc.getPage(request).getWebResponse();

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getContentAsString().contains("message"));
    }

    @Test
    public void testDoConfigure_ValidYaml_NoChanges() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to("admin"));
        WebClient wc = j.createWebClient();
        wc.withBasicApiToken("admin");
        wc.setThrowExceptionOnFailingStatusCode(false);

        WebRequest request1 = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.POST);
        request1.setAdditionalHeader("Content-Type", "application/yaml");
        request1.setRequestBody("jenkins:\n  systemMessage: 'Idempotency Test'");

        WebResponse response1 = wc.getPage(request1).getWebResponse();
        assertEquals(200, response1.getStatusCode());
        assertEquals("Idempotency Test", j.jenkins.getSystemMessage());

        WebRequest request2 = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.POST);
        request2.setAdditionalHeader("Content-Type", "application/yaml");
        request2.setRequestBody("jenkins:\n  systemMessage: 'Idempotency Test'");

        WebResponse response2 = wc.getPage(request2).getWebResponse();
        assertEquals(200, response2.getStatusCode());
        assertEquals("Idempotency Test", j.jenkins.getSystemMessage());
    }
}
