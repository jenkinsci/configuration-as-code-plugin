package io.jenkins.plugins.casc.fetcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

public class FetchCredentialsChainTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testProviderSuccessfullyResolvesCredential() {
        FetchCredentials credentials = FetchCredentials.resolveAll();
        FetchAuthData.Token tokenAuth = credentials.get("dummy-id", FetchAuthData.Token.class);

        assertNotNull(tokenAuth);
        assertEquals("dummy-token-value", tokenAuth.getToken());
    }

    @Test
    public void testProviderExceptionIsIgnoredAndFallsBack() {
        FetchCredentials credentials = FetchCredentials.resolveAll();
        FetchAuthData.Token tokenAuth = credentials.get("error-id", FetchAuthData.Token.class);

        assertNull(tokenAuth);
    }

    @TestExtension
    @SuppressWarnings("unused")
    public static class DummyProvider implements FetchCredentialsProvider {
        @SuppressWarnings("unchecked")
        @Override
        public <T extends FetchAuthData> T getCredentials(String credentialId, Class<T> type) {
            if ("dummy-id".equals(credentialId) && type == FetchAuthData.Token.class) {
                return (T) (FetchAuthData.Token) () -> "dummy-token-value";
            }
            return null;
        }
    }

    @TestExtension
    @SuppressWarnings("unused")
    public static class ExceptionThrowingProvider implements FetchCredentialsProvider {
        @Override
        public <T extends FetchAuthData> T getCredentials(String credentialId, Class<T> type) {
            if ("error-id".equals(credentialId)) {
                throw new RuntimeException("Simulated provider failure");
            }
            return null;
        }
    }
}
