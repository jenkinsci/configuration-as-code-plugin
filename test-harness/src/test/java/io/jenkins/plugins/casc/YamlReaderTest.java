package io.jenkins.plugins.casc;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebRequest;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.yaml.YamlSource;
import io.jenkins.plugins.casc.yaml.YamlUtils;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.MessageFormat;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;

import static com.gargoylesoftware.htmlunit.HttpMethod.POST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

public class YamlReaderTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test(expected = IOException.class)
    public void unknownReader() throws IOException {
        YamlUtils.reader(new YamlSource<>(new StringBuilder()));
    }

    @Test
    public void folder() throws Exception {
        String p = Paths.get(getClass().getResource("./folder").toURI()).toFile().getAbsolutePath();
        ConfigurationAsCode.get().configure(p);
        Jenkins jenkins = Jenkins.get();
        assertEquals("configuration as code - JenkinsConfigTestFolder", jenkins.getSystemMessage());
        assertEquals(10, jenkins.getQuietPeriod());
    }

    @Test
    public void httpDoApply() throws Exception {
        j.jenkins.setCrumbIssuer(null);

        URL apiURL = new URL(MessageFormat.format(
            "{0}configuration-as-code/apply",
            j.getURL().toString()));
        WebRequest request =
            new WebRequest(apiURL, POST);
        request.setCharset(StandardCharsets.UTF_8);
        request.setRequestBody("jenkins:\n"
            + "  systemMessage: \"configuration as code - JenkinsConfigTestHttpRequest\"\n"
            + "  quietPeriod: 10");
        int response = j.createWebClient().getPage(request).getWebResponse().getStatusCode();
        assertThat(response, is(200));
        Jenkins jenkins = Jenkins.get();
        assertEquals("configuration as code - JenkinsConfigTestHttpRequest", jenkins.getSystemMessage());
        assertEquals(10, jenkins.getQuietPeriod());
    }

    @Test
    public void httpDoCheck() throws Exception {
        j.jenkins.setCrumbIssuer(null);

        URL apiURL = new URL(MessageFormat.format(
            "{0}configuration-as-code/check",
            j.getURL().toString()));
        WebRequest request =
            new WebRequest(apiURL, POST);
        request.setCharset(StandardCharsets.UTF_8);
        request.setRequestBody("jenkins:\n"
            + "  systemMessage: \"configuration as code - JenkinsConfigTestHttpRequest\"\n"
            + "  quietPeriod: 10");
        int response = j.createWebClient().getPage(request).getWebResponse().getStatusCode();
        assertThat(response, is(200));
    }

    @Test(expected = FailingHttpStatusCodeException.class)
    public void httpDoCheckFailure() throws Exception {
        j.jenkins.setCrumbIssuer(null);

        URL apiURL = new URL(MessageFormat.format(
            "{0}configuration-as-code/check",
            j.getURL().toString()));
        WebRequest request =
            new WebRequest(apiURL, POST);
        request.setCharset(StandardCharsets.UTF_8);
        request.setRequestBody("jenkins:\n"
            + "  systemMessage: {}");
        j.createWebClient().getPage(request);
    }
}
