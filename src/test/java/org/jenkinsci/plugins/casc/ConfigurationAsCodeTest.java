package org.jenkinsci.plugins.casc;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertNotNull;

public class ConfigurationAsCodeTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void init_test_from_accepted_sources() throws Exception {
        File tmpConfigFile = tempFolder.newFile("jenkins_tmp.yaml");
        assertNotNull(ConfigurationAsCode.getConfigurationInput(tmpConfigFile.getAbsolutePath()));
        assertNotNull(ConfigurationAsCode.getConfigurationInput("file://"+tmpConfigFile.getAbsolutePath()));
    }
}
