package org.jenkinsci.plugins.casc;

import hudson.model.Descriptor;
import hudson.plugins.git.GitTool;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ToolInstallationTest {


    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void set_git_installations() throws Exception {
        ConfigurationAsCode.configure(getClass().getResourceAsStream("Git.yml"));

        final Jenkins jenkins = Jenkins.getInstance();
        final GitTool.DescriptorImpl descriptor = (GitTool.DescriptorImpl) jenkins.getDescriptor(GitTool.class);
        assertEquals(2, descriptor.getInstallations().length);
        assertEquals("/usr/local/bin/git", descriptor.getInstallation("another_git").getGitExe());
        assertEquals("/bin/git", descriptor.getInstallation("git").getGitExe());
    }
}
