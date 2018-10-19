package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        File file = tempFolder.newFile("jenkins_tmp2.yaml");
        FileUtils.writeStringToFile(file, "test");

        tempFolder.newFile("jenkins_tmp3.YAML");
        tempFolder.newFile("jenkins_tmp4.YML");
        tempFolder.newFile("jenkins_tmp5.yml");
        tempFolder.newFolder("jenkins_folder.yml");

        // should be picked up
        Path target = Paths.get("jenkins_tmp2.yaml");
        Path newLink = Paths.get(tempFolder.getRoot().getAbsolutePath(), "symbolic_link_to_tmp2.yaml");
        Files.createSymbolicLink(newLink, target);

        // should not be picked up
        tempFolder.newFolder("jenkins_folder2.yaml");

        assertThat(casc.configs(exactFile.getAbsolutePath()), hasSize(1));
        assertThat(casc.configs(tempFolder.getRoot().getAbsolutePath()), hasSize(6));
    }

    @Test(expected = ConfiguratorException.class)
    public void shouldReportMissingFileOnNotFoundConfig() throws ConfiguratorException {
        ConfigurationAsCode casc = new ConfigurationAsCode();
        casc.configure("some");
    }

    @Test
    @ConfiguredWithCode(value = {"merge1.yml", "merge3.yml"}, expected = ConfiguratorException.class)
    public void shouldMergeYamlConfig() {
        assertEquals("Configured by configuration-as-code-plugin", j.jenkins.getSystemMessage());
        assertEquals(0, j.jenkins.getNumExecutors());
        assertNotNull(j.jenkins.getNode("agent1"));
        assertNotNull(j.jenkins.getNode("agent3"));
    }

    @Test
    @ConfiguredWithCode(value = {"merge1.yml", "merge2.yml"}, expected = ConfiguratorException.class)
    public void shouldReportConfigurationConflict() {
    }
}
