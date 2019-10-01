package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;

/**
 * @author v1v (Victor Martinez)
 */
public class SlackTest {

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvironmentVariables()
        .set("SLACK_TOKEN", "ADMIN123"))
        .around(new JenkinsConfiguredWithReadmeRule());

    @Test
    @ConfiguredWithReadme("slack/README.md")
    public void configure_slack() throws Exception {
        // Already validated within the plugin itself
        // https://github.com/jenkinsci/slack-plugin/pull/582
    }
}
