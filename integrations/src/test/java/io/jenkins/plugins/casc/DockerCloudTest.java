package io.jenkins.plugins.casc;

import com.nirima.jenkins.plugins.docker.DockerCloud;
import com.nirima.jenkins.plugins.docker.DockerTemplate;
import com.nirima.jenkins.plugins.docker.strategy.DockerOnceRetentionStrategy;
import hudson.model.Label;
import io.jenkins.docker.connector.DockerComputerAttachConnector;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DockerCloudTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("docker/README.md")
    public void configure_docker_cloud() throws Exception {
        final DockerCloud docker = DockerCloud.getCloudByName("docker");
        assertNotNull(docker);
        assertNotNull(docker.getDockerApi());
        assertNotNull(docker.getDockerApi().getDockerHost());
        assertEquals("unix:///var/run/docker.sock", docker.getDockerApi().getDockerHost().getUri());

        final DockerTemplate template = docker.getTemplate("jenkins/agent");
        checkTemplate(template, "docker-agent", "jenkins", "/home/jenkins/agent", "10",
                new String[] { "hello:/hello", "world:/world"}, "hello=world\nfoo=bar");
        assertTrue(template.getRetentionStrategy() instanceof DockerOnceRetentionStrategy);
        assertEquals(1, ((DockerOnceRetentionStrategy) template.getRetentionStrategy()).getIdleMinutes());
    }

    @Test
    @ConfiguredWithReadme("docker/README.md")
    public void update_docker_cloud() throws Exception {
        DockerCloud docker = DockerCloud.getCloudByName("docker");
        assertNotNull(docker);
        assertNotNull(docker.getDockerApi());
        assertNotNull(docker.getDockerApi().getDockerHost());
        assertEquals("unix:///var/run/docker.sock", docker.getDockerApi().getDockerHost().getUri());

        DockerTemplate template = docker.getTemplate(Label.get("docker-agent"));
        checkTemplate(template, "docker-agent", "jenkins", "/home/jenkins/agent", "10",
                new String[] { "hello:/hello", "world:/world"}, "hello=world\nfoo=bar");

        ConfigurationAsCode.get().configure(getClass().getResource("DockerCloudTest2.yml").toExternalForm());

        docker = DockerCloud.getCloudByName("docker");
        assertNotNull(docker);
        assertNotNull(docker.getDockerApi());
        assertNotNull(docker.getDockerApi().getDockerHost());
        assertEquals("unix:///var/run/docker.sock", docker.getDockerApi().getDockerHost().getUri());

        template = docker.getTemplate(Label.get("docker-agent"));
        checkTemplate(template, "docker-agent", "jenkins", "/home/jenkins/agent", "10",
                new String[] { "hello:/hello", "world:/world"}, "hello=world\nfoo=bar");

        template = docker.getTemplate(Label.get("generic"));
        checkTemplate(template, "generic", "jenkins", "/home/jenkins/agent2", "5",
                new String[] { "hello:/hello", "world:/world"}, "hello=world\nfoo=bar");
    }

    private void checkTemplate(DockerTemplate template, String labelString, String user, String remoteFs,
                               String instanceCapStr, String[] volumes, String environmentsString) {
        assertNotNull(template);
        assertEquals(labelString, template.getLabelString());
        assertEquals(user, ((DockerComputerAttachConnector) template.getConnector()).getUser());
        assertEquals(remoteFs, template.getRemoteFs());
        assertEquals(instanceCapStr, template.getInstanceCapStr());
        assertArrayEquals(volumes, template.getVolumes());
        assertEquals(environmentsString, template.getEnvironmentsString());
    }
}
