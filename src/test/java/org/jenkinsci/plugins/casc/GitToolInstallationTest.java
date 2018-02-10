package org.jenkinsci.plugins.casc;

import hudson.plugins.git.GitTool;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.misc.CodeConfiguratorRunner;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class GitToolInstallationTest {

    public JenkinsRule j = new JenkinsRule();
    public CodeConfiguratorRunner config = new CodeConfiguratorRunner();

    @Rule
    public RuleChain chain = RuleChain.outerRule(j).around(config);

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
