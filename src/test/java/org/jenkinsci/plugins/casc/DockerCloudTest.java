package org.jenkinsci.plugins.casc;

import com.nirima.jenkins.plugins.docker.DockerCloud;
import com.nirima.jenkins.plugins.docker.DockerTemplate;
import hudson.model.Label;
import io.jenkins.docker.connector.DockerComputerAttachConnector;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DockerCloudTest {


    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void configure_docker_cloud() throws Exception {
        ConfigurationAsCode.configure(getClass().getResourceAsStream("DockerCloudTest.yml"));

        final DockerCloud docker = DockerCloud.getCloudByName("docker");
        assertNotNull(docker);
        assertNotNull(docker.getDockerApi());
        assertNotNull(docker.getDockerApi().getDockerHost());
        assertEquals("unix:///var/run/docker.sock", docker.getDockerApi().getDockerHost().getUri());
        final DockerTemplate template = docker.getTemplate("jenkins/slave");
        checkTemplate(template, "docker-agent", "jenkins", "/home/jenkins/agent", "10");
    }

    @Test
    public void update_docker_cloud() throws Exception {
        ConfigurationAsCode.configure(getClass().getResourceAsStream(
                "DockerCloudTest/update_docker_cloud/DockerCloudTest1.yml"));

        DockerCloud docker = DockerCloud.getCloudByName("docker");
        assertNotNull(docker);
        assertNotNull(docker.getDockerApi());
        assertNotNull(docker.getDockerApi().getDockerHost());
        assertEquals("unix:///var/run/docker.sock", docker.getDockerApi().getDockerHost().getUri());

        DockerTemplate template = docker.getTemplate(Label.get("docker-agent"));
        checkTemplate(template, "docker-agent", "jenkins", "/home/jenkins/agent", "10");

        ConfigurationAsCode.configure(getClass().getResourceAsStream(
                "DockerCloudTest/update_docker_cloud/DockerCloudTest2.yml"));

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
                               String instanceCapStr){
        assertNotNull(template);
        assertEquals(labelString, template.getLabelString());
        assertEquals(user, ((DockerComputerAttachConnector) template.getConnector()).getUser());
        assertEquals(remoteFs, template.getRemoteFs());
        assertEquals(instanceCapStr, template.getInstanceCapStr());
    }
}
