package io.jenkins.plugins.casc.fetcher;

import static org.junit.Assert.assertNull;

import org.junit.Test;

public class FetchCredentialsEarlyBootTest {

    @Test
    public void testJenkinsNullFallsBackToEnvResolver() {
        FetchCredentials credentials = FetchCredentials.resolveAll();
        FetchAuthData.Token tokenAuth = credentials.get("some-id", FetchAuthData.Token.class);

        assertNull(tokenAuth);
    }
}
