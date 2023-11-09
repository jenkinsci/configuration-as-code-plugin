package io.jenkins.plugins.casc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

public class ErrorPageTest {

    @ClassRule
    public static JenkinsRule r = new JenkinsRule();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path cascFile;

    @Before
    public void setup() throws IOException {
        cascFile = folder.newFile("jenkins.yaml").toPath();
    }

    @Test
    public void reloadSimple() throws Exception {
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
            HtmlForm reload = htmlPage.getFormByName("reload");
            HtmlPage submit = r.submit(reload);

            return submit.asNormalizedText();
        } finally {
            System.clearProperty(ConfigurationAsCode.CASC_JENKINS_CONFIG_PROPERTY);
        }
    }

    @Test
    public void noConfigurator() throws Exception {
        Files.write(cascFile, "invalid:\n  systemMessage2: Hello World\n".getBytes());

        String pageContent = reloadConfiguration();
        assertThat(pageContent, containsString("No configurator for the following root elements"));
        assertThat(pageContent, containsString("invalid"));
    }

    @Test
    public void noImplementationFoundForSymbol() throws Exception {
        String content = "jenkins:\n" + "  securityRealm:\n" + "    unknown:\n" + "      username: \"world\"\n";

        Files.write(cascFile, content.getBytes());

        String pageContent = reloadConfiguration();
        assertThat(pageContent, containsString("No implementation found for:"));
        assertThat(pageContent, containsString("securityRealm"));
        assertThat(pageContent, containsString("Attribute was:"));
        assertThat(pageContent, containsString("unknown"));
    }
}
