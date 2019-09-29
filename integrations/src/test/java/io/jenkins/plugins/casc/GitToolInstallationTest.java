package io.jenkins.plugins.casc;

import hudson.plugins.git.GitTool;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import io.jenkins.plugins.casc.model.CNode;
import jenkins.model.Jenkins;
import org.junit.ClassRule;
import org.junit.Test;

import static io.jenkins.plugins.casc.misc.Util.getToolRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class GitToolInstallationTest {

    @ClassRule
    @ConfiguredWithReadme("git/README.md")
    public static JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    public void configure_git_installations() throws Exception {
        final Jenkins jenkins = Jenkins.get();
        final GitTool.DescriptorImpl descriptor = (GitTool.DescriptorImpl) jenkins.getDescriptor(GitTool.class);
        assertEquals(2, descriptor.getInstallations().length);
        assertEquals("/usr/local/bin/git", descriptor.getInstallation("another_git").getGitExe());
        assertEquals("/bin/git", descriptor.getInstallation("git").getGitExe());
    }

    @Test
    public void export_git_installations() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getToolRoot(context).get("git");

        String exported = toYamlString(yourAttribute);

        String expected = toStringFromYamlFile(this, "GitToolInstallationTestExpected.yml");

        assertThat(exported, is(expected));
    }
}
