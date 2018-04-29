package org.jenkinsci.plugins.casc;

import com.nirima.jenkins.plugins.docker.DockerCloud;
import com.nirima.jenkins.plugins.docker.DockerTemplate;
import hudson.model.Label;
import io.jenkins.docker.connector.DockerComputerAttachConnector;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DockerCloudTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("DockerCloudTest.yml")
    public void configure_docker_cloud() throws Exception {
        final DockerCloud docker = DockerCloud.getCloudByName("docker");
        assertNotNull(docker);
        assertNotNull(docker.getDockerApi());
        assertNotNull(docker.getDockerApi().getDockerHost());
        assertEquals("unix:///var/run/docker.sock", docker.getDockerApi().getDockerHost().getUri());
        final DockerTemplate template = docker.getTemplate("jenkins/slave");
        checkTemplate(template, "docker-agent", "jenkins", "/home/jenkins/agent", "10");
    }

    @Test
    @ConfiguredWithCode("DockerCloudTest/update_docker_cloud/DockerCloudTest1.yml")
    public void update_docker_cloud() throws Exception {
        DockerCloud docker = DockerCloud.getCloudByName("docker");
        assertNotNull(docker);
        assertNotNull(docker.getDockerApi());
        assertNotNull(docker.getDockerApi().getDockerHost());
        assertEquals("unix:///var/run/docker.sock", docker.getDockerApi().getDockerHost().getUri());

        DockerTemplate template = docker.getTemplate(Label.get("docker-agent"));
        checkTemplate(template, "docker-agent", "jenkins", "/home/jenkins/agent", "10");

        ConfigurationAsCode.get().configure(getClass().getResource("DockerCloudTest/update_docker_cloud/DockerCloudTest2.yml").toExternalForm());

        docker = DockerCloud.getCloudByName("docker");
        assertNotNull(docker);
        assertNotNull(docker.getDockerApi());
        assertNotNull(docker.getDockerApi().getDockerHost());
        assertEquals("unix:///var/run/docker.sock", docker.getDockerApi().getDockerHost().getUri());

        template = docker.getTemplate(Label.get("docker-agent"));
        checkTemplate(template, "docker-agent", "jenkins", "/home/jenkins/agent", "10");

        template = docker.getTemplate(Label.get("generic"));
        checkTemplate(template, "generic", "jenkins", "/home/jenkins/agent2", "5");

    }

    private void checkTemplate(DockerTemplate template, String labelString, String user, String remoteFs,
                               String instanceCapStr) {
        assertNotNull(template);
        assertEquals(labelString, template.getLabelString());
        assertEquals(user, ((DockerComputerAttachConnector) template.getConnector()).getUser());
        assertEquals(remoteFs, template.getRemoteFs());
        assertEquals(instanceCapStr, template.getInstanceCapStr());
    }
}
