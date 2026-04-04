package io.jenkins.plugins.casc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.ServletRequest;
import java.net.URL;
import jenkins.model.Jenkins;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

public class ConfigurationAsCodeApiTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testDoConfigure_DisabledByDefault() throws Exception {
        System.clearProperty(ConfigurationAsCode.CASC_ALLOW_HTTP_POST_CONFIG);

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(jenkins.model.Jenkins.ADMINISTER)
                .everywhere()
                .to("admin"));

        WebClient wc = j.createWebClient();
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        wc.login("admin", "admin");
        wc.setThrowExceptionOnFailingStatusCode(false);

        WebRequest request = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.POST);
        request.setAdditionalHeader("Content-Type", "application/yaml");
        request.setRequestBody("jenkins:\n  systemMessage: 'Testing'");

        WebResponse response = wc.getPage(request).getWebResponse();

        assertEquals(403, response.getStatusCode());
    }

    @Test
    public void testDoConfigure_RequiresPost() throws Exception {
        System.setProperty(ConfigurationAsCode.CASC_ALLOW_HTTP_POST_CONFIG, "true");

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(jenkins.model.Jenkins.ADMINISTER)
                .everywhere()
                .to("admin"));

        WebClient wc = j.createWebClient();
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        wc.login("admin", "admin");
        wc.setThrowExceptionOnFailingStatusCode(false);

        WebRequest request = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.GET);
        WebResponse response = wc.getPage(request).getWebResponse();

        assertTrue(response.getStatusCode() == 404 || response.getStatusCode() == 405);

        System.clearProperty(ConfigurationAsCode.CASC_ALLOW_HTTP_POST_CONFIG);
    }

    @Test
    public void testDoConfigure_Success() throws Exception {
        System.setProperty(ConfigurationAsCode.CASC_ALLOW_HTTP_POST_CONFIG, "true");

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(jenkins.model.Jenkins.ADMINISTER)
                .everywhere()
                .to("admin"));

        WebClient wc = j.createWebClient();
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        wc.login("admin", "admin");
        wc.setThrowExceptionOnFailingStatusCode(false);

        WebRequest request = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.POST);
        request.setAdditionalHeader("Content-Type", "application/yaml");
        request.setRequestBody("jenkins:\n  systemMessage: 'Webhook Success'");

        var crumbIssuer = j.jenkins.getCrumbIssuer();

        if (crumbIssuer != null) {
            request.setAdditionalHeader(
                    crumbIssuer.getCrumbRequestField(), crumbIssuer.getCrumb((ServletRequest) null));
        }

        WebResponse response = wc.getPage(request).getWebResponse();

        assertEquals(200, response.getStatusCode());
        assertEquals("Webhook Success", j.jenkins.getSystemMessage());

        System.clearProperty(ConfigurationAsCode.CASC_ALLOW_HTTP_POST_CONFIG);
    }

    @Test
    public void testDoConfigure_InvalidYaml() throws Exception {
        System.setProperty(ConfigurationAsCode.CASC_ALLOW_HTTP_POST_CONFIG, "true");

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(jenkins.model.Jenkins.ADMINISTER)
                .everywhere()
                .to("admin"));

        WebClient wc = j.createWebClient();
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        wc.login("admin", "admin");
        wc.setThrowExceptionOnFailingStatusCode(false);

        WebRequest request = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.POST);

        request.setRequestBody("jenkins:\n  systemMessage: [invalid");

        var crumbIssuer = j.jenkins.getCrumbIssuer();

        if (crumbIssuer != null) {
            request.setAdditionalHeader(
                    crumbIssuer.getCrumbRequestField(), crumbIssuer.getCrumb((ServletRequest) null));
        }

        WebResponse response = wc.getPage(request).getWebResponse();

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getContentAsString().contains("message"));

        System.clearProperty(ConfigurationAsCode.CASC_ALLOW_HTTP_POST_CONFIG);
    }

    @Test
    public void testDoConfigure_NonAdminForbidden() throws Exception {
        System.setProperty(ConfigurationAsCode.CASC_ALLOW_HTTP_POST_CONFIG, "true");

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ)
                .everywhere()
                .to("user")
                .grant(jenkins.model.Jenkins.ADMINISTER)
                .everywhere()
                .to("admin"));

        WebClient wc = j.createWebClient();
        wc.login("user", "user");
        wc.setThrowExceptionOnFailingStatusCode(false);

        WebRequest request = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.POST);
        request.setRequestBody("jenkins:\n  systemMessage: 'fail'");

        var crumbIssuer = j.jenkins.getCrumbIssuer();

        if (crumbIssuer != null) {
            request.setAdditionalHeader(
                    crumbIssuer.getCrumbRequestField(), crumbIssuer.getCrumb((ServletRequest) null));
        }

        WebResponse response = wc.getPage(request).getWebResponse();

        assertEquals(403, response.getStatusCode());

        System.clearProperty(ConfigurationAsCode.CASC_ALLOW_HTTP_POST_CONFIG);
    }

    @Test
    public void testDoConfigure_Unauthenticated() throws Exception {
        System.setProperty(ConfigurationAsCode.CASC_ALLOW_HTTP_POST_CONFIG, "true");

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new org.jvnet.hudson.test.MockAuthorizationStrategy()
                .grant(Jenkins.READ)
                .everywhere()
                .toEveryone());

        WebClient wc = j.createWebClient();
        wc.setThrowExceptionOnFailingStatusCode(false);

        WebRequest request = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.POST);

        request.setRequestBody("jenkins:\n  systemMessage: 'anonymous bypass attempt'");

        WebResponse response = wc.getPage(request).getWebResponse();

        assertEquals(403, response.getStatusCode());

        System.clearProperty(ConfigurationAsCode.CASC_ALLOW_HTTP_POST_CONFIG);
    }

    @Test
    public void testDoConfigure_MissingCrumb() throws Exception {
        System.setProperty(ConfigurationAsCode.CASC_ALLOW_HTTP_POST_CONFIG, "true");

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to("admin"));

        WebClient wc = j.createWebClient();
        wc.login("admin", "admin");
        wc.setThrowExceptionOnFailingStatusCode(false);

        WebRequest request = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.POST);

        request.setRequestBody("jenkins:\n  systemMessage: 'no crumb'");

        WebResponse response = wc.getPage(request).getWebResponse();

        assertEquals(403, response.getStatusCode());
        assertTrue(response.getContentAsString().contains("No valid crumb"));

        System.clearProperty(ConfigurationAsCode.CASC_ALLOW_HTTP_POST_CONFIG);
    }

    @Test
    public void testDoConfigure_EmptyBody() throws Exception {
        System.setProperty(ConfigurationAsCode.CASC_ALLOW_HTTP_POST_CONFIG, "true");

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to("admin"));

        WebClient wc = j.createWebClient();
        wc.login("admin", "admin");

        wc.setThrowExceptionOnFailingStatusCode(false);

        WebRequest request = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.POST);
        request.setAdditionalHeader("Content-Type", "application/yaml");

        var crumbIssuer = j.jenkins.getCrumbIssuer();

        if (crumbIssuer != null) {
            request.setAdditionalHeader(
                    crumbIssuer.getCrumbRequestField(), crumbIssuer.getCrumb((ServletRequest) null));
        }

        WebResponse response = wc.getPage(request).getWebResponse();

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getContentAsString().contains("message"));
    }

    @After
    public void tearDown() {
        System.clearProperty(ConfigurationAsCode.CASC_ALLOW_HTTP_POST_CONFIG);
    }

    @Test
    public void testDoConfigure_MalformedStructure() throws Exception {
        System.setProperty(ConfigurationAsCode.CASC_ALLOW_HTTP_POST_CONFIG, "true");
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(jenkins.model.Jenkins.ADMINISTER)
                .everywhere()
                .to("admin"));

        WebClient wc = j.createWebClient();
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        wc.login("admin", "admin");
        wc.setThrowExceptionOnFailingStatusCode(false);

        WebRequest request = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.POST);
        request.setAdditionalHeader("Content-Type", "application/yaml");
        request.setRequestBody("jenkins:\n  invalidRoot:\n    foo: bar");

        var crumbIssuer = j.jenkins.getCrumbIssuer();

        if (crumbIssuer != null) {
            request.setAdditionalHeader(
                    crumbIssuer.getCrumbRequestField(), crumbIssuer.getCrumb((ServletRequest) null));
        }

        WebResponse response = wc.getPage(request).getWebResponse();

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getContentAsString().contains("message"));
    }

    @Test
    public void testDoConfigure_ValidYaml_NoChanges() throws Exception {
        System.setProperty(ConfigurationAsCode.CASC_ALLOW_HTTP_POST_CONFIG, "true");

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to("admin"));

        WebClient wc = j.createWebClient();
        wc.login("admin", "admin");
        wc.setThrowExceptionOnFailingStatusCode(false);

        WebRequest request1 = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.POST);
        request1.setAdditionalHeader("Content-Type", "application/yaml");
        request1.setRequestBody("jenkins:\n  systemMessage: 'Idempotency Test'");
        var crumbIssuer = j.jenkins.getCrumbIssuer();
        if (crumbIssuer != null) {
            request1.setAdditionalHeader(
                    crumbIssuer.getCrumbRequestField(), crumbIssuer.getCrumb((ServletRequest) null));
        }

        WebResponse response1 = wc.getPage(request1).getWebResponse();
        assertEquals(200, response1.getStatusCode());
        assertEquals("Idempotency Test", j.jenkins.getSystemMessage());

        WebRequest request2 = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.POST);
        request2.setAdditionalHeader("Content-Type", "application/yaml");
        request2.setRequestBody("jenkins:\n  systemMessage: 'Idempotency Test'");
        if (crumbIssuer != null) {
            request2.setAdditionalHeader(
                    crumbIssuer.getCrumbRequestField(), crumbIssuer.getCrumb((ServletRequest) null));
        }

        WebResponse response2 = wc.getPage(request2).getWebResponse();

        assertEquals(200, response2.getStatusCode());
        assertEquals("Idempotency Test", j.jenkins.getSystemMessage());
    }

    @Test
    public void testDoConfigure_WithApiToken_NoCrumb() throws Exception {
        System.setProperty(ConfigurationAsCode.CASC_ALLOW_HTTP_POST_CONFIG, "true");

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
            .grant(Jenkins.ADMINISTER)
            .everywhere()
            .to("admin"));

        WebClient wc = j.createWebClient();

        wc.withBasicApiToken("admin");
        wc.setThrowExceptionOnFailingStatusCode(false);

        WebRequest request = new WebRequest(new URL(j.getURL(), "configuration-as-code/configure"), HttpMethod.POST);
        request.setAdditionalHeader("Content-Type", "application/yaml");
        request.setRequestBody("jenkins:\n  systemMessage: 'API Token Success'");

        WebResponse response = wc.getPage(request).getWebResponse();

        assertEquals(200, response.getStatusCode());
        assertEquals("API Token Success", j.jenkins.getSystemMessage());

        System.clearProperty(ConfigurationAsCode.CASC_ALLOW_HTTP_POST_CONFIG);
    }
}
