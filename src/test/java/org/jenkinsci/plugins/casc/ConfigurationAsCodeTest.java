package org.jenkinsci.plugins.casc;

import hudson.plugins.git.GitTool;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

public class ConfigurationAsCodeTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void init_test_from_accepted_sources() throws Exception {
        ConfigurationAsCode casc = new ConfigurationAsCode();

        File exactFile = tempFolder.newFile("jenkins_tmp.yaml");
        tempFolder.newFile("jenkins_tmp2.yaml");
        tempFolder.newFile("jenkins_tmp3.YAML");
        tempFolder.newFile("jenkins_tmp4.YML");
        tempFolder.newFile("jenkins_tmp5.yml");
        tempFolder.newFolder("jenkins_folder.yml");

        assertThat(casc.configs(exactFile.getAbsolutePath()), hasSize(1));
        assertThat(casc.configs(tempFolder.getRoot().getAbsolutePath()), hasSize(5));
    }

    @Test(expected = ConfiguratorException.class)
    public void shouldReportMissingFileOnNotFoundConfig() throws ConfiguratorException {
        ConfigurationAsCode casc = new ConfigurationAsCode();
        casc.configure("some");
    }

    @Test
    @ConfiguredWithCode(value = { "merge1.yml", "merge3.yml"}, expected = ConfiguratorException.class)
    public void shouldMergeYamlConfig() {
        assertEquals("Configured by configuration-as-code-plugin", j.jenkins.getSystemMessage());
        assertEquals(0, j.jenkins.getNumExecutors());
        GitTool.DescriptorImpl tools = j.jenkins.getDescriptorByType(GitTool.DescriptorImpl.class);
        assertNotNull(tools.getInstallation("git"));
        assertNotNull(tools.getInstallation("anothergit"));
    }

    @Test
    @ConfiguredWithCode(value = { "merge1.yml", "merge2.yml"}, expected = ConfiguratorException.class)
    public void shouldReportConfigurationConflict() {
    }
}
