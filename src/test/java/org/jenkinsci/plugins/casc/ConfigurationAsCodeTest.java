package org.jenkinsci.plugins.casc;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class ConfigurationAsCodeTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void init_test_from_accepted_sources() throws Exception {
        File tmpConfigFile = tempFolder.newFile("jenkins_tmp.yaml");
        tempFolder.newFile("jenkins_tmp2.yaml");
        assertEquals(1, ConfigurationAsCode.getConfigurationInput(tmpConfigFile.getAbsolutePath()).size());
        assertEquals(2, ConfigurationAsCode.getConfigurationInput(tempFolder.getRoot().getAbsolutePath()).size());
    }
}
