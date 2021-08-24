package io.jenkins.plugins.casc.yaml;

import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import net.sf.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static com.gargoylesoftware.htmlunit.HttpMethod.GET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MergeStrategyActionTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void test() throws IOException {
        URL apiURL = new URL(MessageFormat.format(
            "{0}cascMergeStrategy",
            j.getURL().toString()));
        WebRequest request = new WebRequest(apiURL, GET);

        WebResponse response = j.createWebClient().getPage(request)
            .getWebResponse();
        String strategies = response.getContentAsString();

        JSONObject strategiesJSON = JSONObject.fromObject(strategies);
        assertEquals("The request should be ok", "ok", strategiesJSON.getString("status"));
        assertNotNull("Should have data field", strategiesJSON.getJSONArray("data"));
        for (Object item : strategiesJSON.getJSONArray("data")) {
            String name = JSONObject.fromObject(item).getString("name");
            assertEquals(name, MergeStrategyFactory.getMergeStrategyOrDefault(name).getName());
        }
    }
}
