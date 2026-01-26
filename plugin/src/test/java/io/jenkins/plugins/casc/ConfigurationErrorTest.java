package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.yaml.YamlSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.JenkinsExtension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(JenkinsExtension.class)
class ConfigurationErrorTest {

    @TempDir
    Path tempDir;

    @Test
    @SuppressWarnings("unused")
    void shouldReportLineNumberAndAttributeForTypeMismatch(JenkinsRule j) throws Exception {
        String yaml =
            "jenkins:\n" +
                "  systemMessage: [\"Wrong Type\"]";

        File configFile = tempDir.resolve("bad-config.yaml").toFile();
        Files.writeString(configFile.toPath(), yaml);

        YamlSource<String> source = YamlSource.of(configFile.toURI().toString());

        ConfiguratorException ex = assertThrows(
            ConfiguratorException.class,
            () -> ConfigurationAsCode.get().configureWith(source)
        );

        assertThat(ex.getPath(), equalTo("systemMessage"));
        assertThat(ex.getSource(), notNullValue());
        assertThat(ex.getSource().toString(), containsString("2"));
    }
}
