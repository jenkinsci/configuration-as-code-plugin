package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.plugins.git.GitSCMSource;
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries;
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration;
import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:VictorMartinezRubio@gmail.com">Victor Martinez</a>
 */
public class WorkflowCpsGlobalLibTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j2 = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("workflow-cps-global-lib/README.md")
    public void configure_global_library() throws Exception {
        assertEquals(1, GlobalLibraries.get().getLibraries().size());
        final LibraryConfiguration library = GlobalLibraries.get().getLibraries().get(0);
        assertEquals("awesome-lib", library.getName());
        final SCMSourceRetriever retriever = (SCMSourceRetriever) library.getRetriever();
        final GitSCMSource scm = (GitSCMSource) retriever.getScm();
        assertEquals("https://github.com/jenkins-infra/pipeline-library.git", scm.getRemote());
    }
}
