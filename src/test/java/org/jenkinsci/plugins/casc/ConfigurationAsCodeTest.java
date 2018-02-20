package org.jenkinsci.plugins.casc;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class ConfigurationAsCodeTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

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

    @Test
    public void shouldReturnEmptyMapOnNotFoundConfig() {
        ConfigurationAsCode casc = new ConfigurationAsCode();
        assertThat(casc.configs("some"), hasSize(0));
    }
}
