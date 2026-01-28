package io.jenkins.plugins.casc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

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
}
