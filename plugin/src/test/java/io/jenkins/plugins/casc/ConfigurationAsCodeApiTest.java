package io.jenkins.plugins.casc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.net.URL;
import jenkins.model.Jenkins;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

public class ConfigurationAsCodeApiTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private static final String ENDPOINT = "configuration-as-code/configure";
    private static final String YAML_CONTENT_TYPE = "application/yaml";
    private static final String ADMIN = "admin";

    private WebRequest webRequest(HttpMethod method) throws Exception {
        return new WebRequest(new URL(j.getURL(), ENDPOINT), method);
    }

    private WebRequest yamlPost(String requestBody) throws Exception {
        WebRequest request = webRequest(HttpMethod.POST);
        request.setAdditionalHeader("Content-Type", YAML_CONTENT_TYPE);
        if (requestBody != null) {
            request.setRequestBody(requestBody);
        }
        return request;
    }

    private void configureAdminSecurity() {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to(ADMIN));
    }

    @Test
    public void testDoConfigure_RequiresPost() throws Exception {
        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            wc.setThrowExceptionOnFailingStatusCode(false);

            WebResponse response = wc.getPage(webRequest(HttpMethod.GET)).getWebResponse();
            assertThat(response.getStatusCode(), is(405));
        }
    }

    @Test
    public void testDoConfigure_Success() throws Exception {
        configureAdminSecurity();

        try (JenkinsRule.WebClient wc = j.createWebClient().withBasicApiToken(ADMIN)) {
            wc.setThrowExceptionOnFailingStatusCode(false);

            WebResponse response = wc.getPage(yamlPost("jenkins:\n  systemMessage: 'Webhook Success'"))
                    .getWebResponse();

            assertThat(response.getStatusCode(), is(200));
            assertThat(j.jenkins.getSystemMessage(), is("Webhook Success"));
        }
    }

    @Test
    public void testDoConfigure_InvalidYaml() throws Exception {
        configureAdminSecurity();

        try (JenkinsRule.WebClient wc = j.createWebClient().withBasicApiToken(ADMIN)) {
            wc.setThrowExceptionOnFailingStatusCode(false);

            WebResponse response =
                    wc.getPage(yamlPost("jenkins:\n  systemMessage: [invalid")).getWebResponse();

            assertThat(response.getStatusCode(), is(400));
            assertThat(response.getContentAsString(), containsString("message"));
        }
    }

    @Test
    public void testDoConfigure_NonAdminForbidden() throws Exception {
        try (JenkinsRule.WebClient wc = j.createWebClient().withBasicApiToken("user")) {
            wc.setThrowExceptionOnFailingStatusCode(false);

            WebRequest request = webRequest(HttpMethod.POST);
            request.setRequestBody("jenkins:\n  systemMessage: 'fail'");

            WebResponse response = wc.getPage(request).getWebResponse();
            assertThat(response.getStatusCode(), is(403));
        }
    }

    @Test
    public void testDoConfigure_Unauthenticated() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(
                new MockAuthorizationStrategy().grant(Jenkins.READ).everywhere().toEveryone());

        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            wc.setThrowExceptionOnFailingStatusCode(false);

            WebRequest request = webRequest(HttpMethod.POST);
            request.setRequestBody("jenkins:\n  systemMessage: 'anonymous bypass attempt'");

            WebResponse response = wc.getPage(request).getWebResponse();
            assertThat(response.getStatusCode(), is(403));
        }
    }

    @Test
    public void testDoConfigure_EmptyBody() throws Exception {
        configureAdminSecurity();

        try (JenkinsRule.WebClient wc = j.createWebClient().withBasicApiToken(ADMIN)) {
            wc.setThrowExceptionOnFailingStatusCode(false);

            WebResponse response = wc.getPage(yamlPost(null)).getWebResponse();

            assertThat(response.getStatusCode(), is(400));
            assertThat(response.getContentAsString(), containsString("message"));
        }
    }

    @Test
    public void testDoConfigure_MalformedStructure() throws Exception {
        configureAdminSecurity();

        try (JenkinsRule.WebClient wc = j.createWebClient().withBasicApiToken(ADMIN)) {
            wc.setThrowExceptionOnFailingStatusCode(false);

            WebResponse response = wc.getPage(yamlPost("jenkins:\n  invalidRoot:\n    foo: bar"))
                    .getWebResponse();

            assertThat(response.getStatusCode(), is(400));
            assertThat(response.getContentAsString(), containsString("message"));
        }
    }

    @Test
    public void testDoConfigure_ValidYaml_NoChanges() throws Exception {
        configureAdminSecurity();

        try (JenkinsRule.WebClient wc = j.createWebClient().withBasicApiToken(ADMIN)) {
            wc.setThrowExceptionOnFailingStatusCode(false);

            WebResponse response1 = wc.getPage(yamlPost("jenkins:\n  systemMessage: 'Idempotency Test'"))
                    .getWebResponse();
            assertThat(response1.getStatusCode(), is(200));
            assertThat(j.jenkins.getSystemMessage(), is("Idempotency Test"));

            WebResponse response2 = wc.getPage(yamlPost("jenkins:\n  systemMessage: 'Idempotency Test'"))
                    .getWebResponse();
            assertThat(response2.getStatusCode(), is(200));
            assertThat(j.jenkins.getSystemMessage(), is("Idempotency Test"));
        }
    }
}
