package org.jenkinsci.plugins.casc;

import com.nirima.jenkins.plugins.docker.DockerCloud;
import com.nirima.jenkins.plugins.docker.DockerTemplate;
import com.nirima.jenkins.plugins.docker.launcher.AttachedDockerComputerLauncher;
import hudson.plugins.active_directory.ActiveDirectoryDomain;
import hudson.plugins.active_directory.ActiveDirectorySecurityRealm;
import io.jenkins.docker.connector.DockerComputerAttachConnector;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        final DockerTemplate template = docker.getTemplate("jenkins/slave");
        assertNotNull(template);
        assertEquals("docker-agent", template.getLabelString());
        assertEquals("jenkins", ((DockerComputerAttachConnector) template.getConnector()).getUser());
    }
}
