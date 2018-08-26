package io.jenkins.plugins.casc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig;
import org.jenkinsci.plugins.configfiles.groovy.GroovyScript;
import org.jenkinsci.plugins.configfiles.json.JsonConfig;
import org.jenkinsci.plugins.configfiles.maven.GlobalMavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig;
import org.jenkinsci.plugins.configfiles.maven.MavenToolchainsConfig;
import org.jenkinsci.plugins.configfiles.maven.security.ServerCredentialMapping;
import org.jenkinsci.plugins.configfiles.xml.XmlConfig;
import org.junit.Rule;
import org.junit.Test;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.support.configfiles.GlobalConfigFilesConfigurator;

/**
 * Tests to verify {@link GlobalConfigFilesConfigurator}.
 *
 * @author srempfer
 */
public class GlobalConfigFilesTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @ConfiguredWithCode("GlobalConfigFiles-GlobalMavenSettingsConfig.yml")
    @Test
    public void testWithGlobalMavenSettingsConfig() throws IOException {

        Collection<Config> configs = GlobalConfigFiles.get().getConfigs();
        assertThat(configs, hasSize(1));

        Config element = configs.iterator().next();

        Class<? extends Config> configType = element.getClass();
        assertThat(configType, equalTo(GlobalMavenSettingsConfig.class));

        GlobalMavenSettingsConfig config = (GlobalMavenSettingsConfig) element;

        assertThat(config.id, equalTo("bd78669c-5d69-47b0-91d4-b1a075365b6a"));
        assertThat(config.name, equalTo("DummyGlobalSettings"));
        assertThat(config.comment, equalTo("dummy global settings"));
        assertThat(config.isReplaceAll, equalTo(Boolean.TRUE));

        List<ServerCredentialMapping> serverCredentialMappings = config.getServerCredentialMappings();
        for (ServerCredentialMapping mapping : serverCredentialMappings) {

            String serverId = mapping.getServerId();
            assertThat(serverId, anyOf(equalTo("server1"), equalTo("server2")));

            if ("server1".equals(serverId)) {
                assertThat(mapping.getCredentialsId(), equalTo("credentialsA"));
            } else {
                assertThat(mapping.getCredentialsId(), equalTo("credentialsZ"));
            }
        }

        assertContentFromFile(config, "GlobalConfigFiles-MavenSettings.content");
    }

    @ConfiguredWithCode("GlobalConfigFiles-MavenSettingsConfig.yml")
    @Test
    public void testWithlMavenSettingsConfig() throws IOException {

        Collection<Config> configs = GlobalConfigFiles.get().getConfigs();
        assertThat(configs, hasSize(1));

        Config element = configs.iterator().next();

        Class<? extends Config> configType = element.getClass();
        assertThat(configType, equalTo(MavenSettingsConfig.class));

        MavenSettingsConfig config = (MavenSettingsConfig) element;

        assertThat(config.id, equalTo("d40b9db5-3369-4c63-968e-290fd6614009"));
        assertThat(config.name, equalTo("DummySettings"));
        assertThat(config.comment, equalTo("dummy settings"));
        assertThat(config.isReplaceAll, equalTo(Boolean.TRUE));

        List<ServerCredentialMapping> serverCredentialMappings = config.getServerCredentialMappings();
        for (ServerCredentialMapping mapping : serverCredentialMappings) {

            String serverId = mapping.getServerId();
            assertThat(serverId, anyOf(equalTo("server3"), equalTo("server4")));

            if ("server3".equals(serverId)) {
                assertThat(mapping.getCredentialsId(), equalTo("credentialsB"));
            } else {
                assertThat(mapping.getCredentialsId(), equalTo("credentialsY"));
            }
        }

        assertContentFromFile(config, "GlobalConfigFiles-MavenSettings.content");

    }

    @ConfiguredWithCode("GlobalConfigFiles-MavenToolchainsConfig.yml")
    @Test
    public void testWithMavenToolchainsConfig() throws IOException {

        Collection<Config> configs = GlobalConfigFiles.get().getConfigs();
        assertThat(configs, hasSize(1));

        Config element = configs.iterator().next();

        Class<? extends Config> configType = element.getClass();
        assertThat(configType, equalTo(MavenToolchainsConfig.class));

        MavenToolchainsConfig config = (MavenToolchainsConfig) element;

        assertThat(config.id, equalTo("87961ed8-b97a-4132-98d9-eeba9f81624f"));
        assertThat(config.name, equalTo("DummyToolchains"));
        assertThat(config.comment, equalTo("dummy toolchains"));

        assertContentFromFile(config, "GlobalConfigFiles-MavenToolchains.content");

    }


    private void assertContentFromFile(Config config, String file) throws IOException {
        File f = new File(GlobalConfigFilesTest.class.getResource(file).getFile());
        String contentFromFile = FileUtils.readFileToString(f, StandardCharsets.UTF_8);

        // normalize line endings because of Windows-Linux differences
        String configContent = StringUtils.replace ( config.content, "\r\n", "\n" );
        String expectedContent = StringUtils.replace ( contentFromFile, "\r\n", "\n" );

        assertThat(configContent, equalTo(expectedContent));
    }

    @ConfiguredWithCode("GlobalConfigFiles-CustomConfig.yml")
    @Test
    public void testWithCustomConfig() throws IOException {

        Collection<Config> configs = GlobalConfigFiles.get().getConfigs();
        assertThat(configs, hasSize(2));

        for (Config element : configs) {

            Class<? extends Config> configType = element.getClass();
            assertThat(configType, equalTo(CustomConfig.class));

            CustomConfig config = (CustomConfig) element;

            assertThat(config.id, anyOf(equalTo("68168f02-ad9e-4729-b83f-7b59432be774"), equalTo("7dd3b657-547f-419f-9988-3a53b0cf9db6")));

            if ("68168f02-ad9e-4729-b83f-7b59432be774".equals(config.id)) {
                assertThat(config.name, equalTo("DummyCustom1"));
                assertThat(config.comment, equalTo("dummy custom 1"));
                assertThat(config.content, equalTo("dummy content 1"));

            } else {
                assertThat(config.name, equalTo("DummyCustom2"));
                assertThat(config.comment, equalTo("dummy custom 2"));
                assertThat(config.content, equalTo("dummy content 2"));

            }
        }

    }

    @ConfiguredWithCode("GlobalConfigFiles-GroovyScript.yml")
    @Test
    public void testWithGroovyScriptConfig() throws IOException {

        Collection<Config> configs = GlobalConfigFiles.get().getConfigs();
        assertThat(configs, hasSize(1));

        Config element = configs.iterator().next();

        Class<? extends Config> configType = element.getClass();
        assertThat(configType, equalTo(GroovyScript.class));

        GroovyScript config = (GroovyScript) element;

        assertThat(config.id, equalTo("ff8503b1-c4e7-4e52-9b8e-2ed774448c88"));
        assertThat(config.name, equalTo("DummyGroovyConfig"));
        assertThat(config.comment, equalTo("dummy groovy config"));
        assertThat(config.content, equalTo("println('dummy hello world')"));
    }

    @ConfiguredWithCode("GlobalConfigFiles-JsonConfig.yml")
    @Test
    public void testWithJsonConfig() throws IOException {

        Collection<Config> configs = GlobalConfigFiles.get().getConfigs();
        assertThat(configs, hasSize(1));

        Config element = configs.iterator().next();

        Class<? extends Config> configType = element.getClass();
        assertThat(configType, equalTo(JsonConfig.class));

        JsonConfig config = (JsonConfig) element;

        assertThat(config.id, equalTo("3c9bc804-3ae8-4026-a3a3-1192bf564422"));
        assertThat(config.name, equalTo("DummyJsonConfig"));
        assertThat(config.comment, equalTo("dummy json config"));
        assertThat(config.content, equalTo("{ \"dummydata\": {\"dummyKey\": \"dummyValue\"} }"));
    }

    @ConfiguredWithCode("GlobalConfigFiles-XmlConfig.yml")
    @Test
    public void testWithXmlConfig() throws IOException {

        Collection<Config> configs = GlobalConfigFiles.get().getConfigs();
        assertThat(configs, hasSize(1));

        Config element = configs.iterator().next();

        Class<? extends Config> configType = element.getClass();
        assertThat(configType, equalTo(XmlConfig.class));

        XmlConfig config = (XmlConfig) element;

        assertThat(config.id, equalTo("ef61bcee-506a-47e7-9325-73857d7441a3"));
        assertThat(config.name, equalTo("DummyXmlConfig"));
        assertThat(config.comment, equalTo("dummy xml config"));
        assertThat(config.content, equalTo("<root><dummy test=\"abc\"></dummy></root>"));
    }
}
