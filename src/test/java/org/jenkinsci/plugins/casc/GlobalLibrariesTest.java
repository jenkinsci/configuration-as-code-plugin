package org.jenkinsci.plugins.casc;

import com.nirima.jenkins.plugins.docker.DockerCloud;
import com.nirima.jenkins.plugins.docker.DockerTemplate;
import io.jenkins.docker.connector.DockerComputerAttachConnector;
import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMSource;
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries;
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration;
import org.jenkinsci.plugins.workflow.libs.LibraryRetriever;
import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class GlobalLibrariesTest {


    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void configure_global_library() throws Exception {
        ConfigurationAsCode.configure(getClass().getResourceAsStream("GlobalLibrariesTest.yml"));

        assertEquals(1, GlobalLibraries.get().getLibraries().size());
        final LibraryConfiguration library = GlobalLibraries.get().getLibraries().get(0);
        assertEquals("awesome-lib", library.getName());
        final SCMSourceRetriever retriever = (SCMSourceRetriever) library.getRetriever();
        final GitSCMSource scm = (GitSCMSource) retriever.getScm();
        assertEquals("https://github.com/jenkins-infra/pipeline-library.git", scm.getRemote());

    }
}
