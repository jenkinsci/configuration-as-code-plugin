package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class GlobalConfigFilesTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("GlobalConfigFilesTest.yml")
    public void should_set_config_files() throws Exception {
        final GlobalConfigFiles cfg = GlobalConfigFiles.get();
        final Config custom = cfg.getById("custom-test");
        assertNotNull(custom);
        assertEquals("dummy content 1", custom.content);
        final Config json = cfg.getById("json-test");
        assertNotNull(json);
        assertEquals("{ \"dummydata\": {\"dummyKey\": \"dummyValue\"} }", json.content);
        final Config xml = cfg.getById("xml-test");
        assertNotNull(xml);
        assertEquals("<root><dummy test=\"abc\"></dummy></root>", xml.content);
        final MavenSettingsConfig maven = (MavenSettingsConfig) cfg.getById("maven-test");
        assertNotNull(maven);
        assertFalse(maven.isReplaceAll);
        assertEquals("someCredentials", maven.getServerCredentialMappings().get(0).getCredentialsId());
    }

}