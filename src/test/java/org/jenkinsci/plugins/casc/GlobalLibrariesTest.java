package org.jenkinsci.plugins.casc;

import jenkins.plugins.git.GitSCMSource;
import org.jenkinsci.plugins.casc.misc.CodeConfiguratorRunner;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries;
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration;
import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class GlobalLibrariesTest {


    public JenkinsRule j = new JenkinsRule();
    public CodeConfiguratorRunner config = new CodeConfiguratorRunner();

    @Rule
    public RuleChain chain = RuleChain.outerRule(j).around(config);


    @Test
    @ConfiguredWithCode("GlobalLibrariesTest.yml")
    public void configure_global_library() throws Exception {
        assertEquals(1, GlobalLibraries.get().getLibraries().size());
        final LibraryConfiguration library = GlobalLibraries.get().getLibraries().get(0);
        assertEquals("awesome-lib", library.getName());
        final SCMSourceRetriever retriever = (SCMSourceRetriever) library.getRetriever();
        final GitSCMSource scm = (GitSCMSource) retriever.getScm();
        assertEquals("https://github.com/jenkins-infra/pipeline-library.git", scm.getRemote());

    }
}
