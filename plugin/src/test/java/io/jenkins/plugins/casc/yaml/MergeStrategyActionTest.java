package io.jenkins.plugins.casc.yaml;

import static org.htmlunit.HttpMethod.GET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import net.sf.json.JSONObject;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class MergeStrategyActionTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void test() throws IOException {
        URL apiURL =
                new URL(MessageFormat.format("{0}cascMergeStrategy", j.getURL().toString()));
        WebRequest request = new WebRequest(apiURL, GET);

        WebResponse response = j.createWebClient().getPage(request).getWebResponse();
        String strategies = response.getContentAsString();

        JSONObject strategiesJSON = JSONObject.fromObject(strategies);
        assertEquals("The request should be ok", "ok", strategiesJSON.getString("status"));
        assertNotNull("Should have data field", strategiesJSON.getJSONArray("data"));
        for (Object item : strategiesJSON.getJSONArray("data")) {
            String name = JSONObject.fromObject(item).getString("name");
            assertEquals(
                    name, MergeStrategyFactory.getMergeStrategyOrDefault(name).getName());
        }
    }
}
