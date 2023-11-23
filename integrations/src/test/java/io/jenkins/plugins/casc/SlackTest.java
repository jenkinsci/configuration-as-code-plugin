package io.jenkins.plugins.casc;

import static io.jenkins.plugins.casc.misc.Util.convertYamlFileToJson;
import static io.jenkins.plugins.casc.misc.Util.validateSchema;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import hudson.ExtensionList;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.plugins.slack.SlackNotifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;

/**
 * @author v1v (Victor Martinez)
 */
public class SlackTest {
    @Before
    public void shouldThisRun() {
        assumeTrue(ShouldRun.thisTest());
    }

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvironmentVariables().set("SLACK_TOKEN", "ADMIN123"))
            .around(new JenkinsConfiguredWithReadmeRule());

    @Test
    @ConfiguredWithReadme("slack/README.md")
    public void configure_slack() {
        // Already validated within the plugin itself, so let's run some simple validations
        // https://github.com/jenkinsci/slack-plugin/pull/582
        SlackNotifier.DescriptorImpl slackNotifier = ExtensionList.lookupSingleton(SlackNotifier.DescriptorImpl.class);
        assertNotNull(slackNotifier);
    }

    @Test
    public void validJsonSchema() throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "slackSchema.yml")), empty());
    }
}
