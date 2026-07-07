package io.jenkins.plugins.casc.fetcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.util.Secret;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class JenkinsCredentialsProviderTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private CredentialsStore systemStore;
    private JenkinsCredentialsProvider provider;

    @Before
    public void setUp() {
        systemStore = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        provider = new JenkinsCredentialsProvider();
    }

    @Test
    public void testResolveSecretTextToToken() throws Exception {
        StringCredentialsImpl stringCred = new StringCredentialsImpl(
                com.cloudbees.plugins.credentials.CredentialsScope.GLOBAL,
                "my-secret-text-id",
                "Test description",
                Secret.fromString("super-secret-token-123"));
        systemStore.addCredentials(Domain.global(), stringCred);

        FetchAuthData.Token tokenAuth = provider.getCredentials("my-secret-text-id", FetchAuthData.Token.class);

        assertNotNull("Token credentials should be successfully bridged", tokenAuth);
        assertEquals("super-secret-token-123", tokenAuth.getToken());
    }

    @Test
    public void testResolveUsernamePassword() throws Exception {
        UsernamePasswordCredentialsImpl userPassCred = new UsernamePasswordCredentialsImpl(
                com.cloudbees.plugins.credentials.CredentialsScope.GLOBAL,
                "my-user-pass-id",
                "Test description",
                "jenkins-admin",
                "secure-password");
        systemStore.addCredentials(Domain.global(), userPassCred);

        FetchAuthData.UsernamePassword userPassAuth =
                provider.getCredentials("my-user-pass-id", FetchAuthData.UsernamePassword.class);

        assertNotNull(userPassAuth);
        assertEquals("jenkins-admin", userPassAuth.getUsername());
        assertEquals("secure-password", userPassAuth.getPassword());
    }

    @Test
    public void testResolveSshPrivateKey() throws Exception {
        BasicSSHUserPrivateKey sshCred = new BasicSSHUserPrivateKey(
                com.cloudbees.plugins.credentials.CredentialsScope.GLOBAL,
                "my-ssh-key-id",
                "git-user",
                new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(
                        "-----BEGIN RSA PRIVATE KEY-----\nMOCK-KEY\n-----END RSA PRIVATE KEY-----"),
                "passphrase-xyz",
                "Test description");
        systemStore.addCredentials(Domain.global(), sshCred);

        FetchAuthData.SshKey sshAuth = provider.getCredentials("my-ssh-key-id", FetchAuthData.SshKey.class);

        assertNotNull(sshAuth);
        assertEquals("git-user", sshAuth.getUsername());
        assertEquals(
                "-----BEGIN RSA PRIVATE KEY-----\nMOCK-KEY\n-----END RSA PRIVATE KEY-----",
                sshAuth.getPrivateKey().trim());
        assertEquals("passphrase-xyz", sshAuth.getPassphrase());
    }

    @Test
    public void testReturnNullOnNonExistentCredential() {
        FetchAuthData.Token missing = provider.getCredentials("invalid-id", FetchAuthData.Token.class);
        assertNull("Should return null cleanly if credential ID doesn't exist", missing);
    }

    @Test
    public void testCredentialTypeMismatchReturnsNull() throws Exception {
        StringCredentialsImpl stringCred = new StringCredentialsImpl(
                com.cloudbees.plugins.credentials.CredentialsScope.GLOBAL,
                "mismatch-id",
                "Test description",
                Secret.fromString("secret"));
        assertTrue(systemStore.addCredentials(Domain.global(), stringCred));

        FetchAuthData.SshKey mismatchAuth = provider.getCredentials("mismatch-id", FetchAuthData.SshKey.class);

        assertNull("Should safely return null on type mismatch without throwing exceptions", mismatchAuth);
    }
}
