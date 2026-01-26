package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.yaml.YamlSource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.nio.file.Files;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;

public class ConfigurationErrorTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void shouldReportLineNumberAndAttributeForTypeMismatch() throws Exception {
        String yaml = "jenkins:\n" +
            "  systemMessage: [\"Wrong Type\"]";

        File configFile = tempFolder.newFile("bad-config.yaml");
        Files.writeString(configFile.toPath(), yaml);

        YamlSource<String> source = YamlSource.of(configFile.toURI().toString());

        try {
            ConfigurationAsCode.get().configureWith(source);
            fail("Should have thrown ConfiguratorException");
        } catch (ConfiguratorException e) {
            assertThat("Path should contain 'systemMessage'", e.getPath(),
                equalTo("systemMessage"));
            assertThat("Source should not be null", e.getSource(), notNullValue());
            assertThat("Error message should contain line 2", e.getSource().toString(),
                containsString("2"));
        }
    }
}
