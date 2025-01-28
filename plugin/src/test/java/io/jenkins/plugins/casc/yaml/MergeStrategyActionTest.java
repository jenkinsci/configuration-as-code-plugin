package io.jenkins.plugins.casc.yaml;

import static org.htmlunit.HttpMethod.GET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import net.sf.json.JSONObject;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class MergeStrategyActionTest {

    @Test
    void test(JenkinsRule j) throws IOException {
        URL apiURL =
                new URL(MessageFormat.format("{0}cascMergeStrategy", j.getURL().toString()));
        WebRequest request = new WebRequest(apiURL, GET);

        WebResponse response = j.createWebClient().getPage(request).getWebResponse();
        String strategies = response.getContentAsString();

        JSONObject strategiesJSON = JSONObject.fromObject(strategies);
        assertEquals("ok", strategiesJSON.getString("status"), "The request should be ok");
        assertNotNull(strategiesJSON.getJSONArray("data"), "Should have data field");
        for (Object item : strategiesJSON.getJSONArray("data")) {
            String name = JSONObject.fromObject(item).getString("name");
            assertEquals(
                    name, MergeStrategyFactory.getMergeStrategyOrDefault(name).getName());
        }
    }
}
