package io.jenkins.plugins.casc;

import hudson.plugins.git.GitTool;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class GitToolInstallationTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("GitToolInstallationTest.yml")
    public void configure_git_installations() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();
        final GitTool.DescriptorImpl descriptor = (GitTool.DescriptorImpl) jenkins.getDescriptor(GitTool.class);
        assertEquals(2, descriptor.getInstallations().length);
        assertEquals("/usr/local/bin/git", descriptor.getInstallation("another_git").getGitExe());
        assertEquals("/bin/git", descriptor.getInstallation("git").getGitExe());
    }
}
