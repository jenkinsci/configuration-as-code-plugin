package org.jenkinsci.plugins.casc;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ConfigurationAsCodeTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void init_test_from_accepted_sources() throws Exception {
        ConfigurationAsCode casc = ConfigurationAsCode.get();
        File tmpConfigFile = tempFolder.newFile("jenkins_tmp.yaml");
        tempFolder.newFile("jenkins_tmp2.yaml");
        assertEquals(1, casc.getConfigurationInputs(tmpConfigFile.getAbsolutePath()).size());
        assertEquals(2, casc.getConfigurationInputs(tempFolder.getRoot().getAbsolutePath()).size());
    }

    @Test
    public void init_read_plugins_first() throws  Exception {
        tempFolder.newFile("Ajenkins1.yaml");
        tempFolder.newFile("jenkins2.yaml");
        tempFolder.newFile("Zplsugins.yaml");
        File withContent = tempFolder.newFile("plugins.yaml");
        FileUtils.writeStringToFile(withContent, "plugins:");
        Map<String,InputStream> cfgs = ConfigurationAsCode.get().getConfigurationInputs(tempFolder.getRoot().getAbsolutePath());
        assertEquals("We expect four elements",4, cfgs.size());
    }
}
