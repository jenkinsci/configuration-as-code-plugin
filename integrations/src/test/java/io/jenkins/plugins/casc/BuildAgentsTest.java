package io.jenkins.plugins.casc;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import hudson.model.Node.Mode;
import hudson.model.Slave;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.JNLPLauncher;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import java.util.Objects;
import org.junit.Rule;
import org.junit.Test;

public class BuildAgentsTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme(value = "build_agents/README.md")
    public void configure_build_agents() {
        assertThat(j.getInstance().getComputers().length, is(3));

        Slave slave = (Slave) j.getInstance().getNode("utility-node");
        assertThat(slave.getRemoteFS(), is("/home/user1"));
        JNLPLauncher jnlpLauncher = ((JNLPLauncher) slave.getLauncher());
        assertThat(jnlpLauncher.getWorkDirSettings().getWorkDirPath(), is("/tmp"));
        assertThat(jnlpLauncher.getWorkDirSettings().getInternalDir(), is("remoting2"));
        assertTrue(jnlpLauncher.getWorkDirSettings().isDisabled());
        assertFalse(jnlpLauncher.getWorkDirSettings().isFailIfWorkDirIsMissing());
        assertTrue(jnlpLauncher.isWebSocket());
        assertThat(jnlpLauncher.tunnel, is("some.proxy"));

        assertThat(j.getInstance().getNode("utility-node-2").getNumExecutors(), is(4));
        assertThat(j.getInstance().getNode("utility-node-2").getMode(), is(Mode.NORMAL));
        slave = (Slave) j.getInstance().getNode("utility-node-2");
        assertThat(slave.getRemoteFS(), is("/home/user2"));
        SSHLauncher launcher = ((SSHLauncher) slave.getLauncher());
        assertThat(launcher.getHost(), is("192.168.1.1"));
        assertThat(launcher.getPort(), is(22));
        assertThat(launcher.getCredentialsId(), is("test"));
        assertThat(launcher.getMaxNumRetries(), is(3));
        assertThat(launcher.getRetryWaitTime(), is(30));
    }

    @Test
    @ConfiguredWithReadme(value = "build_agents/README.md#1")
    public void configure_build_agent_labels() {
        DumbSlave javaNode = (DumbSlave) j.getInstance().getNode("java-node");
        assertThat(Objects.requireNonNull(javaNode).getLabelString(), is("linux docker java maven"));

        DumbSlave pythonNode = (DumbSlave) j.getInstance().getNode("python-node");
        assertThat(Objects.requireNonNull(pythonNode).getLabelString(), is("linux docker python pytest"));
    }
}
