package io.jenkins.plugins.casc.yaml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class YamlSourceTest {

    @Test
    public void shouldHaveInformativeToStringForUrlSource() throws MalformedURLException {
        //given
        String testUrl = "http://example.com/foo/bar";
        //and
        YamlSource<String> yamlSource = YamlSource.of(new URL(testUrl));
        //expect
        assertEquals("YamlSource: " + testUrl, yamlSource.toString());
    }

    @Test
    public void shouldUseToStringOfSourceInToStringForInputStream() {
        //given
        InputStream testInputStream = new ByteArrayInputStream("IS content".getBytes(StandardCharsets.UTF_8));
        String testInputStreamToString = testInputStream.toString();
        //and
        YamlSource<InputStream> yamlSource = YamlSource.of(testInputStream);
        //expect
        assertEquals("YamlSource: " + testInputStreamToString, yamlSource.toString());
    }
}
