package io.jenkins.plugins.casc;

import hudson.model.Node.Mode;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import java.util.List;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.mesos.MesosCloud;
import org.jenkinsci.plugins.mesos.MesosSlaveInfo;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author v1v (Victor Martinez)
 */
public class MesosTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("mesos/README.md")
    public void configure_mesos_cloud() throws Exception {
        final MesosCloud cloud = Jenkins.get().clouds.get(MesosCloud.class);
        assertNotNull(cloud);

        assertFalse(cloud.isCheckpoint());
        assertThat(cloud.getCloudID(), is("mesos-name"));
        assertThat(cloud.getCredentialsId(), is("MESOS_CREDENTIALS"));
        assertThat(cloud.getDeclineOfferDuration(), is("600"));
        assertThat(cloud.getDescription(), is("My Mesos Cloud"));
        assertThat(cloud.getFrameworkName(), is("Jenkins Framework"));
        assertThat(cloud.getJenkinsURL(), is("https://jenkins.mesos.cloud"));
        assertThat(cloud.getMaster(), is("1.2.3.4:8000"));
        assertThat(cloud.getRole(), is("*"));
        assertThat(cloud.getSlavesUser(), is("jenkins"));
        assertThat(cloud.getSlavesUser(), is("jenkins"));

        final List<MesosSlaveInfo> slaves = cloud.getSlaveInfos();
        assertThat(slaves, hasSize(1));

        final MesosSlaveInfo slaveInfo = slaves.get(0);
        assertThat(slaveInfo.getLabelString(), is("docker"));
        assertThat(slaveInfo.getContainerInfo().getType(), is("DOCKER"));
        assertThat(slaveInfo.getContainerInfo().getDockerImage(), is("cloudbees/java-with-docker-client:latest"));
        assertThat(slaveInfo.getContainerInfo().getVolumes(), hasSize(2));
        assertThat(slaveInfo.getMinExecutors(), is(1));
        assertThat(slaveInfo.getMaxExecutors(), is(2));
        assertThat(slaveInfo.getMode(), is(Mode.NORMAL));
        assertThat(slaveInfo.getSlaveCpus(), is(0.1));
        assertThat(slaveInfo.getSlaveMem(), is(512));
        assertThat(slaveInfo.getRemoteFSRoot(), is("jenkins"));
        assertThat(slaveInfo.getJnlpArgs(), is(""));
        assertThat(slaveInfo.getSlaveAttributes().toString(), containsString("\"rack\":\"jenkins-build-agents\""));
        assertTrue(slaveInfo.isDefaultSlave());
    }
}
