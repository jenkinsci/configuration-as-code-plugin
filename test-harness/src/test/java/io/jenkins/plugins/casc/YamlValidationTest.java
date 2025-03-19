package io.jenkins.plugins.casc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import net.sf.json.JSONArray;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;

/**
 * Tests for YAML validation in the Configuration-as-Code endpoint.
 */
@WithJenkinsConfiguredWithCode
public class YamlValidationTest {

    /**
     * Tests that valid YAML returns an empty array response.
     */
    @Test
    void validYaml(JenkinsConfiguredWithCodeRule j) throws Exception {
        // Disable CSRF protection to simplify testing
        j.jenkins.setCrumbIssuer(null);

        WebResponse response = performYamlValidation(j, "jenkins:\n  systemMessage: \"Hello from test\"");

        assertThat(response.getStatusCode(), is(200));
        assertThat(response.getContentType(), is("application/json"));

        JSONArray jsonResponse = JSONArray.fromObject(response.getContentAsString());
        assertThat(jsonResponse.size(), is(0));
    }

    /**
     * Tests that YAML with incorrect indentation returns proper JSON response.
     */
    @Test
    @Issue("2628")
    void invalidIndentation(JenkinsConfiguredWithCodeRule j) throws Exception {
        j.jenkins.setCrumbIssuer(null);

        WebResponse response = performYamlValidation(j, "jenkins:\nsystemMessage: \"Bad indentation\"");

        assertThat(response.getContentType(), is("application/json"));

        // Verify the response contains valid JSON regardless of status code
        String responseString = response.getContentAsString();
        JSONArray jsonResponse = JSONArray.fromObject(responseString);

        // The response should be a non-empty JSON array
        assertThat("Should return error details", jsonResponse.size() > 0, is(true));
    }

    /**
     * Tests that YAML with invalid structure returns proper JSON response.
     */
    @Test
    @Issue("2628")
    void invalidStructure(JenkinsConfiguredWithCodeRule j) throws Exception {
        j.jenkins.setCrumbIssuer(null);

        WebResponse response = performYamlValidation(j, "jenkins:\n  numExecutors: {");

        assertThat(response.getContentType(), is("application/json"));

        // Verify the response contains valid JSON regardless of status code
        String responseString = response.getContentAsString();
        JSONArray jsonResponse = JSONArray.fromObject(responseString);

        // The response should be a non-empty JSON array
        assertThat("Should return error details", jsonResponse.size() > 0, is(true));
    }

    /**
     * Tests that YAML with unknown property returns proper JSON response.
     */
    @Test
    @Issue("2628")
    void unknownProperty(JenkinsConfiguredWithCodeRule j) throws Exception {
        j.jenkins.setCrumbIssuer(null);

        WebResponse response = performYamlValidation(
                j,
                "jenkins:\n  nonExistentProperty: \"This property doesn't exist\"\n  systemMessage: \"Valid property\"");

        assertThat(response.getContentType(), is("application/json"));

        // Verify the response contains valid JSON regardless of status code
        String responseString = response.getContentAsString();
        JSONArray jsonResponse = JSONArray.fromObject(responseString);

        // The response should be a non-empty JSON array with details about the unknown property
        assertThat("Should return error details", jsonResponse.size() > 0, is(true));
        assertThat(responseString, containsString("nonExistentProperty"));
    }

    /**
     * Helper method to perform YAML validation.
     */
    private WebResponse performYamlValidation(JenkinsConfiguredWithCodeRule j, String yamlContent) throws Exception {
        URL apiURL = new URL(MessageFormat.format(
                "{0}configuration-as-code/check", j.getURL().toString()));
        WebRequest request = new WebRequest(apiURL, HttpMethod.POST);
        request.setCharset(StandardCharsets.UTF_8);
        request.setRequestBody(yamlContent);

        WebClient client = j.createWebClient();
        // Don't throw exceptions for error status codes
        client.getOptions().setThrowExceptionOnFailingStatusCode(false);

        return client.getPage(request).getWebResponse();
    }
}
