package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import org.jenkins.plugins.statistics.gatherer.StatisticsConfiguration;
import org.junit.Rule;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class StatisticsGathererTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("statistics-gatherer/README.md")
    public void configure_statistics() throws Exception {
        StatisticsConfiguration config = StatisticsConfiguration.get();
        assertNotNull(config);

        assertThat(config.getBuildUrl(), containsString("http://elasticsearch:9200/jenkins-stats/builds"));
        assertTrue(config.getShouldSendApiHttpRequests());
        assertTrue(config.getBuildInfo());
        assertFalse(config.getQueueInfo());
        assertFalse(config.getProjectInfo());
        assertFalse(config.getBuildStepInfo());
        assertTrue(config.getScmCheckoutInfo());
    }
}
