package io.jenkins.plugins.casc;

import hudson.logging.LogRecorder;
import hudson.logging.LogRecorder.Target;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import io.jenkins.plugins.casc.model.CNode;
import java.util.List;
import java.util.logging.Level;
import org.junit.ClassRule;
import org.junit.Test;

import static io.jenkins.plugins.casc.misc.Util.getJenkinsRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LogRecorderTest {

    @ClassRule
    @ConfiguredWithReadme("log-recorder/README.md")
    public static JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    public void configure_logRecorder() {
        List<LogRecorder> logRecorders = j.jenkins.getLog().getRecorders();

        assertThat(logRecorders.size(), is(1));
        LogRecorder logRecorder = logRecorders.get(0);
        assertThat(logRecorder, is(new LogRecorder("JCasC")));

        List<LogRecorder.Target> targets = logRecorder.getLoggers();
        assertThat(targets.size(), is(1));
        Target target = targets.get(0);
        assertThat(target.name, is("io.jenkins.plugins.casc"));
        assertThat(target.getLevel(), is(Level.WARNING));
    }

    @Test
    public void export() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getJenkinsRoot(context).get("log");

        String exported = toYamlString(yourAttribute);

        String expected = toStringFromYamlFile(this, "LogRecorderTestExpected.yaml");

        assertThat(exported, is(expected));
    }
}
