package io.jenkins.plugins.casc.fetcher;

import static org.junit.Assert.assertNull;

import org.junit.Test;

public class BootstrapEnvVarCredentialResolverTest {

    private final BootstrapEnvVarCredentialResolver resolver = BootstrapEnvVarCredentialResolver.INSTANCE;

    @Test
    public void testMissingVariablesReturnNull() {
        assertNull(resolver.resolve("NON_EXISTENT_TOKEN", FetchAuthData.Token.class));
        assertNull(resolver.resolve("NON_EXISTENT_SSH", FetchAuthData.SshKey.class));
    }

    @Test
    public void testUnsupportedTypeReturnsNull() {
        FetchAuthData.UsernamePassword unsupported = resolver.resolve("SOME_ID", FetchAuthData.UsernamePassword.class);
        assertNull(unsupported);
    }
}
