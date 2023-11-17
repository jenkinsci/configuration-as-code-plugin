package io.jenkins.plugins.casc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.RealJenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

public class ErrorPageBootTest {

    @Rule
    public RealJenkinsRule r = new RealJenkinsRule();

    @Test
    @LocalData
    @Ignore(
            "Requires https://github.com/jenkinsci/jenkins/pull/8442 and the RealJenkinsRule is not working right now as this plugin is not being loaded")
    public void reloadSimple() throws Throwable {
        r.then(j -> {
            try (WebClient webClient = j.createWebClient().withThrowExceptionOnFailingStatusCode(false)) {
                String pageContent = webClient.goTo("").asNormalizedText();
                System.out.println(pageContent);
                j.pause();

                assertThat(pageContent, containsString("Invalid configuration elements for type"));
                assertThat(pageContent, containsString("systemMessage2"));
                assertThat(pageContent, containsString("systemMessage"));
            }
        });
    }
}
