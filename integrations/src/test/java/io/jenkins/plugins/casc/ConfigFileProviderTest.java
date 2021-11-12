package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

/**
 * @author <a href="mailto:VictorMartinezRubio@gmail.com">Victor Martinez</a>
 */
public class ConfigFileProviderTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme(value = "config-file-provider/README.md")
    public void configure_config_file_provider() {
        assertThat(GlobalConfigFiles.get().getConfigs(), hasSize(4));

        Config config = GlobalConfigFiles.get().getById("custom-test");
        assertThat(config.name, is("DummyCustom1"));
        assertThat(config.comment, is("dummy custom 1"));
        assertThat(config.content, is("dummy content 1"));

        config = GlobalConfigFiles.get().getById("json-test");
        assertThat(config.name, is("DummyJsonConfig"));
        assertThat(config.comment, is("dummy json config"));
        assertThat(config.content, containsString("{ \"dummydata\": {\"dummyKey\": \"dummyValue\"} }"));

        config = GlobalConfigFiles.get().getById("xml-test");
        assertThat(config.name, is("DummyXmlConfig"));
        assertThat(config.comment, is("dummy xml config"));
        assertThat(config.content, containsString("<root><dummy test=\"abc\"></dummy></root>"));

        MavenSettingsConfig mavenSettings = (MavenSettingsConfig) GlobalConfigFiles.get().getById("maven-test");
        assertThat(mavenSettings.name, is("DummySettings"));
        assertThat(mavenSettings.comment, is("dummy settings"));
        assertThat(mavenSettings.isReplaceAll, is(false));
        assertThat(mavenSettings.getServerCredentialMappings(), hasSize(2));
        assertThat(mavenSettings.getServerCredentialMappings().get(0).getServerId(), is("server1"));
        assertThat(mavenSettings.getServerCredentialMappings().get(0).getCredentialsId(), is("someCredentials1"));
        assertThat(mavenSettings.getServerCredentialMappings().get(1).getServerId(), is("server2"));
        assertThat(mavenSettings.getServerCredentialMappings().get(1).getCredentialsId(), is("someCredentials2"));
        assertThat(mavenSettings.content, containsString("<activeProfiles>"));
    }
}
