package io.jenkins.plugins.casc.fetcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

import io.jenkins.plugins.casc.fetcher.FetchAuthData.Token;
import org.junit.Test;

public class EnvVarFetchCredentialsProviderTest {

    @Test
    public void testGetCredentialsDelegatesToBootstrapResolver() {
        EnvVarFetchCredentialsProvider provider = new EnvVarFetchCredentialsProvider();

        assertNull(provider.getCredentials("NON_EXISTENT_ID", Token.class));

        String expectedToken = System.getenv("PATH");
        assumeTrue(expectedToken != null && !expectedToken.isEmpty());

        Token tokenAuth = provider.getCredentials("PATH", Token.class);

        assertNotNull(tokenAuth);
        assertEquals(expectedToken, tokenAuth.getToken());
    }
}
