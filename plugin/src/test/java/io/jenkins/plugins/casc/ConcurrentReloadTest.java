package io.jenkins.plugins.casc;

import static hudson.cli.CLICommandInvoker.Matcher.failedWith;
import static hudson.cli.CLICommandInvoker.Matcher.succeededSilently;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.cli.CLICommandInvoker;
import io.jenkins.plugins.casc.cli.ApplyConfigurationCommand;
import io.jenkins.plugins.casc.cli.ReloadJCascConfigurationCommand;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import jenkins.model.Jenkins;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.util.NameValuePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ConcurrentReloadTest {

    private static final String ADMIN = "admin";
    private static final String YAML = "jenkins:\n  systemMessage: 'Concurrent reload'\n";

    private JenkinsRule j;

    private ConfigurationAsCode casc;

    @BeforeEach
    void setUp(JenkinsRule j) {
        this.j = j;
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to(ADMIN));
        casc = ConfigurationAsCode.get();
        assertTrue(casc.tryStartReload(), "Simulate a reload that is already running");
    }

    @AfterEach
    void tearDown() {
        casc.finishReload();
    }

    private WebResponse post(WebRequest request) throws Exception {
        try (JenkinsRule.WebClient wc = j.createWebClient().withBasicApiToken(ADMIN)) {
            wc.setThrowExceptionOnFailingStatusCode(false);
            return wc.getPage(request).getWebResponse();
        }
    }

    private WebRequest request(String path) throws Exception {
        return new WebRequest(new URL(j.getURL(), path), HttpMethod.POST);
    }

    private WebRequest yamlRequest(String path) throws Exception {
        WebRequest request = request(path);
        request.setAdditionalHeader("Content-Type", "application/yaml");
        request.setRequestBody(YAML);
        return request;
    }

    private void assertRejected(WebResponse response) {
        assertThat(response.getStatusCode(), is(HttpServletResponse.SC_CONFLICT));
        assertThat(response.getContentAsString(), containsString(ConfigurationAsCode.RELOAD_IN_PROGRESS_MESSAGE));
        assertThat(j.jenkins.getSystemMessage(), is((String) null));
    }

    @Test
    void reloadIsRejected() throws Exception {
        WebRequest request = request("manage/configuration-as-code/reload");
        assertRejected(post(request));
    }

    @Test
    void replaceIsRejected() throws Exception {
        WebRequest request = request("manage/configuration-as-code/replace");
        request.setRequestParameters(List.of(new NameValuePair("_.newSource", "")));
        assertRejected(post(request));
    }

    @Test
    void applyIsRejected() throws Exception {
        WebResponse response = post(yamlRequest("configuration-as-code/apply"));
        assertRejected(response);
        assertThat(response.getContentType(), is("application/json"));
    }

    @Test
    void configureIsRejected() throws Exception {
        WebResponse response = post(yamlRequest("configuration-as-code/configure"));
        assertRejected(response);
        assertThat(response.getContentType(), is("application/json"));
    }

    @Test
    void cliReloadIsRejected() {
        CLICommandInvoker.Result result = new CLICommandInvoker(j, new ReloadJCascConfigurationCommand())
                .asUser(ADMIN)
                .invoke();
        assertThat(result, failedWith(4));
        assertThat(result.stderr(), containsString(ConfigurationAsCode.RELOAD_IN_PROGRESS_MESSAGE));
    }

    @Test
    void cliApplyIsRejected() {
        CLICommandInvoker.Result result = new CLICommandInvoker(j, new ApplyConfigurationCommand())
                .asUser(ADMIN)
                .withStdin(new ByteArrayInputStream(YAML.getBytes(StandardCharsets.UTF_8)))
                .invoke();
        assertThat(result, failedWith(4));
        assertThat(result.stderr(), containsString(ConfigurationAsCode.RELOAD_IN_PROGRESS_MESSAGE));
        assertThat(j.jenkins.getSystemMessage(), is((String) null));
    }

    @Test
    void reloadIsAllowedOnceInProgressReloadFinishes() {
        casc.finishReload();

        CLICommandInvoker.Result result = new CLICommandInvoker(j, new ApplyConfigurationCommand())
                .asUser(ADMIN)
                .withStdin(new ByteArrayInputStream(YAML.getBytes(StandardCharsets.UTF_8)))
                .invoke();
        assertThat(result, succeededSilently());
        assertThat(j.jenkins.getSystemMessage(), is("Concurrent reload"));
        assertTrue(casc.tryStartReload(), "Reload should no longer be marked as in progress");
    }
}
