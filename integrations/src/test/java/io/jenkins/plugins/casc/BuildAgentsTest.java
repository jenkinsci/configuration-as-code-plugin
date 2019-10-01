package io.jenkins.plugins.casc;

import hudson.model.Node.Mode;
import hudson.model.Slave;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.JNLPLauncher;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BuildAgentsTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme(value = "build_agents/README.md")
    public void configure_build_agents() throws Exception {
        assertThat(j.getInstance().getComputers().length, is(3));

        Slave slave = (Slave)j.getInstance().getNode("utility-node");
        assertThat(slave.getRemoteFS(), is("/home/user1"));
        JNLPLauncher jnlpLauncher = ((JNLPLauncher)slave.getLauncher());
        assertThat(jnlpLauncher.getWorkDirSettings().getWorkDirPath(), is("/tmp"));
        assertThat(jnlpLauncher.getWorkDirSettings().getInternalDir(), is("remoting"));
        assertTrue(jnlpLauncher.getWorkDirSettings().isDisabled());
        assertFalse(jnlpLauncher.getWorkDirSettings().isFailIfWorkDirIsMissing());

        assertThat(j.getInstance().getNode("utility-node-2").getNumExecutors(), is(4));
        assertThat(j.getInstance().getNode("utility-node-2").getMode(), is(Mode.NORMAL));
        slave = (Slave)j.getInstance().getNode("utility-node-2");
        assertThat(slave.getRemoteFS(), is("/home/user2"));
        SSHLauncher launcher = ((SSHLauncher)slave.getLauncher());
        assertThat(launcher.getHost(), is("192.168.1.1"));
        assertThat(launcher.getPort(), is(22));
        assertThat(launcher.getCredentialsId(), is("test"));
        assertThat(launcher.getMaxNumRetries(), is(3));
        assertThat(launcher.getRetryWaitTime(), is(30));
    }
}
