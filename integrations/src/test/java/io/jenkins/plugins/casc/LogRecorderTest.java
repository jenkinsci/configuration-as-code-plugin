package io.jenkins.plugins.casc;

import hudson.logging.LogRecorder;
import hudson.logging.LogRecorder.Target;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import java.util.List;
import java.util.logging.Level;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LogRecorderTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("log-recorder/README.md")
    public void configure_logRecorder() throws Exception {
        List<LogRecorder> logRecorders = j.jenkins.getLog().getRecorders();

        assertThat(logRecorders.size(), is(1));
        LogRecorder logRecorder = logRecorders.get(0);
        assertThat(logRecorder, is(new LogRecorder("JCasC")));

        List<LogRecorder.Target> targets = logRecorder.getTargets();
        assertThat(targets.size(), is(1));
        Target target = targets.get(0);
        assertThat(target.name, is("io.jenkins.plugins.casc"));
        assertThat(target.getLevel(), is(Level.WARNING));
    }
}
