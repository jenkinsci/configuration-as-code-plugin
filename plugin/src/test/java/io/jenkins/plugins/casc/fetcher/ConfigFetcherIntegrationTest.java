package io.jenkins.plugins.casc.fetcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

public class ConfigFetcherIntegrationTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testCustomFetcherEndToEndLifecycle() {
        ConfigurationAsCode casc = ConfigurationAsCode.get();

        casc.configure("mock-fetcher://production/jenkins.yaml");

        List<String> activeSources = casc.getSources();
        assertEquals(1, activeSources.size());
        assertEquals("mock-fetcher://production/jenkins.yaml", activeSources.get(0));
        assertTrue("The AutoCloseable lifecycle must invoke close() on our FetchResult", MockConfigFetcher.wasClosed);
    }

    @TestExtension
    public static class MockConfigFetcher implements CasCConfigFetcher {

        public static boolean wasClosed = false;

        @Override
        public boolean supports(String location) {
            return location != null && location.startsWith("mock-fetcher://");
        }

        @Override
        public FetchResult fetch(String location, FetchCredentials credentials) {
            wasClosed = false;

            String dummyYaml = """
                jenkins:
                  systemMessage: "Configured seamlessly via physical Integration Tests!"
                """;

            InputStream contentStream = new ByteArrayInputStream(dummyYaml.getBytes(StandardCharsets.UTF_8));

            ResolvedYaml resolvedItem = new ResolvedYaml("jenkins.yaml", () -> contentStream);

            AutoCloseable resourceTracker = () -> {
                contentStream.close();
                wasClosed = true;
            };

            return new FetchResult(Collections.singletonList(resolvedItem), resourceTracker);
        }
    }
}
