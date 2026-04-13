package io.jenkins.plugins.casc;

import static hudson.model.ManagementLink.Category.CONFIGURATION;
import static jenkins.model.Jenkins.ADMINISTER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlElementUtil;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ErrorPageTest {

    private JenkinsRule r;

    @TempDir
    public File folder;

    private Path cascFile;

    @BeforeEach
    void setup(JenkinsRule r) throws IOException {
        this.r = r;
        cascFile = File.createTempFile("jenkins.yaml", null, folder).toPath();
    }

    @Test
    void reloadSimple() throws Exception {
        Files.write(cascFile, "jenkins:\n  systemMessage2: Hello World\n".getBytes());

        String pageContent = reloadConfiguration();
        assertThat(pageContent, containsString("Invalid configuration elements for type"));
        assertThat(pageContent, containsString("systemMessage2"));
        assertThat(pageContent, containsString("systemMessage"));
    }

    private String reloadConfiguration() throws Exception {
        try (WebClient webClient = r.createWebClient().withThrowExceptionOnFailingStatusCode(false)) {
            System.setProperty(ConfigurationAsCode.CASC_JENKINS_CONFIG_PROPERTY, cascFile.toString());

            HtmlPage htmlPage = webClient.goTo("manage/configuration-as-code/");
            HtmlButton button = (HtmlButton) htmlPage.getElementById("btn-open-apply-configuration");
            HtmlElementUtil.click(button);

            HtmlForm reload = htmlPage.getFormByName("replace");
            HtmlPage submit = r.submit(reload);

            return submit.asNormalizedText();
        } finally {
            System.clearProperty(ConfigurationAsCode.CASC_JENKINS_CONFIG_PROPERTY);
        }
    }

    @Test
    void noConfigurator() throws Exception {
        Files.write(cascFile, "invalid:\n  systemMessage2: Hello World\n".getBytes());

        String pageContent = reloadConfiguration();
        assertThat(pageContent, containsString("No configurator for the following root elements"));
        assertThat(pageContent, containsString("invalid"));
    }

    @Test
    void noImplementationFoundForSymbol() throws Exception {
        String content = """
            jenkins:
              securityRealm:
                unknown:
                  username: "world"
            """;

        Files.write(cascFile, content.getBytes());

        String pageContent = reloadConfiguration();
        assertThat(pageContent, containsString("No implementation found for:"));
        assertThat(pageContent, containsString("securityRealm"));
        assertThat(pageContent, containsString("Attribute was:"));
        assertThat(pageContent, containsString("unknown"));
    }

    @Test
    void replaceWithInvalidSource() throws Exception {
        String pageContent = replaceConfiguration();

        assertThat(
                pageContent, containsString("Source non-existent-file.yaml could not be applied or does not exist."));
    }

    private String replaceConfiguration() throws Exception {
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        r.jenkins.setAuthorizationStrategy(
                new MockAuthorizationStrategy().grant(ADMINISTER).everywhere().to("admin"));

        try (WebClient webClient = r.createWebClient().withThrowExceptionOnFailingStatusCode(false)) {
            webClient.login("admin", "admin");

            HtmlPage htmlPage = webClient.goTo("manage/configuration-as-code/");

            HtmlButton button = (HtmlButton) htmlPage.getElementById("btn-open-apply-configuration");
            HtmlElementUtil.click(button);

            HtmlForm replaceForm = htmlPage.getFormByName("replace");
            replaceForm.getInputByName("_.newSource").setValue("non-existent-file.yaml");

            HtmlPage submit = r.submit(replaceForm);

            return submit.asNormalizedText();
        }
    }

    @Test
    void verifyManagementLinkProperties() {
        ConfigurationAsCode casc = ConfigurationAsCode.get();

        assertEquals(CONFIGURATION, casc.getCategory());

        assertEquals("configuration-as-code", casc.getUrlName());
        assertEquals("Configuration as Code", casc.getDisplayName());
        assertEquals("symbol-logo plugin-configuration-as-code", casc.getIconFileName());
    }
}
