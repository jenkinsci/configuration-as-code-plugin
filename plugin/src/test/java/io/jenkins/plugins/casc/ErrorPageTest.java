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

        try (WebClient webClient = r.createWebClient().withThrowExceptionOnFailingStatusCode(false)) {
            System.setProperty(ConfigurationAsCode.CASC_JENKINS_CONFIG_PROPERTY, cascFile.toString());

            HtmlPage htmlPage = webClient.goTo("manage/configuration-as-code/");
            HtmlForm reload = htmlPage.getFormByName("reload");
            HtmlPage submit = r.submit(reload);

            String text = submit.asNormalizedText();

            assertThat(text, containsString("Invalid configuration elements for configurator with name"));
            assertThat(text, containsString("systemMessage2"));
            assertThat(text, containsString("systemMessage"));

        } finally {
            System.clearProperty(ConfigurationAsCode.CASC_JENKINS_CONFIG_PROPERTY);
        }
    }

    @Test
    public void noConfigurator() throws Exception {
        Files.write(cascFile, "invalid:\n  systemMessage2: Hello World\n".getBytes());

        // TODO not handled well
    }

    @Test
    public void noImplementationFoundForSymbol() throws Exception {
        Files.write(cascFile, "jenkins:\n  securityRealm:\n    unknown:\n blah: \"world\"".getBytes());

        // TODO not handled well
    }
}
