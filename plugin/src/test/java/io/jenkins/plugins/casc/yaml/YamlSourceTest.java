package io.jenkins.plugins.casc.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jenkins.plugins.casc.MockHttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class YamlSourceTest {

    @Test
    void shouldHaveInformativeToStringForUrlSource() {
        // given
        String testUrl = "http://example.com/foo/bar";
        // and
        YamlSource<String> yamlSource = YamlSource.of(testUrl);
        // expect
        assertEquals("YamlSource: " + testUrl, yamlSource.toString());
    }

    @Test
    void shouldUseToStringOfSourceInToStringForInputStream() {
        // given
        InputStream testInputStream = new ByteArrayInputStream("IS content".getBytes(StandardCharsets.UTF_8));
        String testInputStreamToString = testInputStream.toString();
        // and
        YamlSource<InputStream> yamlSource = YamlSource.of(testInputStream);
        // expect
        assertEquals("YamlSource: " + testInputStreamToString, yamlSource.toString());
    }

    @Test
    void shouldHaveInformativeToStringForPathSource() {
        Path path = new File("./test").toPath();
        String testPathToString = path.toString();
        YamlSource<Path> yamlSource = YamlSource.of(path);
        assertEquals("YamlSource: " + testPathToString, yamlSource.toString());
    }

    @Test
    void shouldHaveInformativeToStringForRequestSource() {
        HttpServletRequest request = new MockHttpServletRequest("/configuration-as-code/check");
        YamlSource<HttpServletRequest> yamlSource = YamlSource.of(request);
        assertEquals("YamlSource: /configuration-as-code/check", yamlSource.toString());
    }
}
