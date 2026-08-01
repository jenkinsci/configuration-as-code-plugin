package io.jenkins.plugins.casc.fetcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

import io.jenkins.plugins.casc.fetcher.FetchAuthData.SshKey;
import io.jenkins.plugins.casc.fetcher.FetchAuthData.Token;
import io.jenkins.plugins.casc.fetcher.FetchAuthData.UsernamePassword;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

public class BootstrapEnvVarCredentialResolverTest {

    @Rule
    public final EnvironmentVariables environment = new EnvironmentVariables();

    private final BootstrapEnvVarCredentialResolver resolver = BootstrapEnvVarCredentialResolver.INSTANCE;

    @Test
    public void testResolveWithInvalidInputsReturnsNull() {
        assertNull(resolver.resolve(null, Token.class));
        assertNull(resolver.resolve("", Token.class));
        assertNull(resolver.resolve("VALID_ID", null));
    }

    @Test
    public void testMissingVariablesReturnNull() {
        assertNull(resolver.resolve("NON_EXISTENT_TOKEN", Token.class));
        assertNull(resolver.resolve("NON_EXISTENT_SSH", SshKey.class));
    }

    @Test
    public void testUnsupportedTypeReturnsNull() {
        UsernamePassword unsupported = resolver.resolve("SOME_ID", UsernamePassword.class);
        assertNull(unsupported);
    }

    @Test
    public void testTokenResolutionWithExistingEnvVar() {
        String expectedToken = System.getenv("PATH");

        assumeTrue(expectedToken != null && !expectedToken.isEmpty());

        Token tokenAuth = resolver.resolve("PATH", FetchAuthData.Token.class);

        assertNotNull(tokenAuth);
        assertEquals(expectedToken, tokenAuth.getToken());
    }

    @Test
    public void testSshKeyResolutionWithAllVars() {
        environment.set("MY_SSH_PRIVATE_KEY", "dummy-key-data");
        environment.set("MY_SSH_USERNAME", "custom-user");
        environment.set("MY_SSH_PASSPHRASE", "super-secret");

        SshKey sshKey = resolver.resolve("MY_SSH", SshKey.class);

        assertNotNull(sshKey);
        assertEquals("dummy-key-data", sshKey.getPrivateKey());
        assertEquals("custom-user", sshKey.getUsername());
        assertEquals("super-secret", sshKey.getPassphrase());
    }

    @Test
    public void testSshKeyResolutionWithDefaultUsername() {
        environment.set("DEFAULT_SSH_PRIVATE_KEY", "dummy-key-data-2");

        SshKey sshKey = resolver.resolve("DEFAULT_SSH", SshKey.class);

        assertNotNull(sshKey);
        assertEquals("dummy-key-data-2", sshKey.getPrivateKey());

        assertEquals("git", sshKey.getUsername());

        assertNull(sshKey.getPassphrase());
    }
}
