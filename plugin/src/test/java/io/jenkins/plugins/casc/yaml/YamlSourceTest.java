package io.jenkins.plugins.casc.yaml;

import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

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
        StringBufferInputStream testInputStream = new StringBufferInputStream("IS content");
        String testInputStreamToString = testInputStream.toString();
        //and
        YamlSource<InputStream> yamlSource = YamlSource.of(testInputStream);
        //expect
        assertEquals("YamlSource: " + testInputStreamToString, yamlSource.toString());
    }
}
