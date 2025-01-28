package io.jenkins.plugins.casc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.htmlunit.HttpMethod.POST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import io.jenkins.plugins.casc.yaml.YamlSource;
import io.jenkins.plugins.casc.yaml.YamlUtils;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.MessageFormat;
import jenkins.model.Jenkins;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.WebRequest;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
class YamlReaderTest {

    @Test
    void unknownReader(JenkinsConfiguredWithCodeRule j) {
        assertThrows(IOException.class, () -> YamlUtils.reader(new YamlSource<>(new StringBuilder())));
    }

    @Test
    void folder(JenkinsConfiguredWithCodeRule j) throws Exception {
        String p =
                Paths.get(getClass().getResource("./folder").toURI()).toFile().getAbsolutePath();
        ConfigurationAsCode.get().configure(p);
        Jenkins jenkins = Jenkins.get();
        assertEquals("configuration as code - JenkinsConfigTestFolder", jenkins.getSystemMessage());
        assertEquals(10, jenkins.getQuietPeriod());
    }

    @Test
    void httpDoApply(JenkinsConfiguredWithCodeRule j) throws Exception {
        j.jenkins.setCrumbIssuer(null);

        URL apiURL = new URL(MessageFormat.format(
                "{0}configuration-as-code/apply", j.getURL().toString()));
        WebRequest request = new WebRequest(apiURL, POST);
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
    void httpDoCheck(JenkinsConfiguredWithCodeRule j) throws Exception {
        j.jenkins.setCrumbIssuer(null);

        URL apiURL = new URL(MessageFormat.format(
                "{0}configuration-as-code/check", j.getURL().toString()));
        WebRequest request = new WebRequest(apiURL, POST);
        request.setCharset(StandardCharsets.UTF_8);
        request.setRequestBody("jenkins:\n"
                + "  systemMessage: \"configuration as code - JenkinsConfigTestHttpRequest\"\n"
                + "  quietPeriod: 10");
        int response = j.createWebClient().getPage(request).getWebResponse().getStatusCode();
        assertThat(response, is(200));
    }

    @Test
    void httpDoCheckFailure(JenkinsConfiguredWithCodeRule j) throws Exception {
        j.jenkins.setCrumbIssuer(null);
        URL apiURL = new URL(MessageFormat.format(
                "{0}configuration-as-code/check", j.getURL().toString()));
        WebRequest request = new WebRequest(apiURL, POST);
        request.setCharset(StandardCharsets.UTF_8);
        request.setRequestBody("jenkins:\n" + "  systemMessage: {}");
        assertThrows(
                FailingHttpStatusCodeException.class, () -> j.createWebClient().getPage(request));
    }
}
