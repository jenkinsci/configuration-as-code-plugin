package io.jenkins.plugins.casc.fetcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import hudson.util.FormValidation;
import io.jenkins.plugins.casc.fetcher.CascHttpSettings.AuthMethod;
import io.jenkins.plugins.casc.fetcher.CascHttpSettings.RemoteConfig;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class CascHttpSettingsTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testLongestMatchWins() {
        CascHttpSettings settings = CascHttpSettings.get();
        settings.setRemoteConfigs(Arrays.asList(
                new RemoteConfig("https://example.com/api/", "cred-short", AuthMethod.BASIC),
                new RemoteConfig("https://example.com/api/v2/", "cred-long", AuthMethod.BEARER)));

        RemoteConfig match1 = CascHttpSettings.getConfigForUrl("https://example.com/api/v2/resource");
        assertNotNull(match1);
        assertEquals("cred-long", match1.getCredentialId());

        RemoteConfig match2 = CascHttpSettings.getConfigForUrl("https://example.com/api/v1/resource");
        assertNotNull(match2);
        assertEquals("cred-short", match2.getCredentialId());

        RemoteConfig match3 = CascHttpSettings.getConfigForUrl("https://example.com/other");
        assertNull(match3);
    }

    @Test
    public void testPathBoundaryMatching() {
        CascHttpSettings settings = CascHttpSettings.get();
        settings.setRemoteConfigs(List.of(new RemoteConfig("https://example.com/foo", "cred", AuthMethod.BASIC)));

        assertNotNull(CascHttpSettings.getConfigForUrl("https://example.com/foo/bar"));
        assertNull(CascHttpSettings.getConfigForUrl("https://example.com/foobar"));
    }

    @Test
    public void testPortMatching() {
        CascHttpSettings settings = CascHttpSettings.get();
        settings.setRemoteConfigs(Arrays.asList(
                new RemoteConfig("https://example.com", "cred-https", AuthMethod.BASIC),
                new RemoteConfig("http://example.com:8080", "cred-8080", AuthMethod.BASIC)));

        assertNotNull(CascHttpSettings.getConfigForUrl("https://example.com:443/test"));
        assertNotNull(CascHttpSettings.getConfigForUrl("http://example.com:8080/test"));
        assertNull(CascHttpSettings.getConfigForUrl("https://example.com:8443/test"));
    }

    @Test
    public void testDescriptorValidation() {
        CascHttpSettings.RemoteConfig.DescriptorImpl descriptor = new CascHttpSettings.RemoteConfig.DescriptorImpl();

        assertEquals(FormValidation.Kind.OK, descriptor.doCheckUrlPrefix("https://example.com/path").kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckUrlPrefix("http://localhost:8080/").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUrlPrefix("ftp://example.com").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUrlPrefix("").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUrlPrefix("https:///path").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUrlPrefix("https://example.com?query=1").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUrlPrefix("https://example.com#fragment").kind);
    }

    @Test
    public void testAuthMethodDefaultsToNone() {
        RemoteConfig config = new RemoteConfig("https://example.com", null, null);

        assertEquals(AuthMethod.NONE, config.getAuthMethod());
    }

    @Test
    public void testCredentialValidation() {
        CascHttpSettings.RemoteConfig.DescriptorImpl descriptor = new CascHttpSettings.RemoteConfig.DescriptorImpl();

        assertEquals(FormValidation.Kind.OK, descriptor.doCheckCredentialId("", "NONE").kind);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckCredentialId("", "BASIC").kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckCredentialId("my-credential", "BASIC").kind);
    }

    @Test
    public void testAuthMethods() {
        assertEquals(4, AuthMethod.values().length);
        assertEquals(AuthMethod.NONE, AuthMethod.valueOf("NONE"));
        assertEquals(AuthMethod.BASIC, AuthMethod.valueOf("BASIC"));
        assertEquals(AuthMethod.BEARER, AuthMethod.valueOf("BEARER"));
        assertEquals(AuthMethod.API_KEY, AuthMethod.valueOf("API_KEY"));
    }

    @Test
    public void testSchemeAndHostMatching() {
        CascHttpSettings settings = CascHttpSettings.get();
        settings.setRemoteConfigs(List.of(new RemoteConfig("https://example.com/api", "cred", AuthMethod.BASIC)));

        assertNull(CascHttpSettings.getConfigForUrl("http://example.com/api/config.yaml"));
        assertNull(CascHttpSettings.getConfigForUrl("https://other.example.com/api/config.yaml"));

        assertNotNull(CascHttpSettings.getConfigForUrl("https://example.com/api/config.yaml"));
    }
}
