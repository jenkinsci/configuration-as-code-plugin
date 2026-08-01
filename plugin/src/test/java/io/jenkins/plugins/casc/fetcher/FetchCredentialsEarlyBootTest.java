package io.jenkins.plugins.casc.fetcher;

import static org.junit.Assert.assertNull;

import io.jenkins.plugins.casc.fetcher.FetchAuthData.Token;
import org.junit.Test;

public class FetchCredentialsEarlyBootTest {

    @Test
    public void testJenkinsNullFallsBackToEnvResolver() {
        FetchCredentials credentials = FetchCredentials.resolveAll();
        Token tokenAuth = credentials.get("some-id", Token.class);

        assertNull(tokenAuth);
    }
}
