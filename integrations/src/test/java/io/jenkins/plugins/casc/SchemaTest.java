package io.jenkins.plugins.casc;

import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static com.gargoylesoftware.htmlunit.HttpMethod.POST;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SchemaTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("SchemaTest.yml")
    public void schemaConfigurationTest() throws Exception {
        JenkinsRule.WebClient client = j.createWebClient();
        WebRequest request =
                new WebRequest(client.createCrumbedUrl("configuration-as-code/schema"), POST);
        WebResponse response = client.loadWebResponse(request);
        assertThat(response.getStatusCode(), is(200));

    }

}
