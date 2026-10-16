package io.jenkins.plugins.casc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class DecryptLookupTest {

    @ClassRule
    public static final JenkinsRule j = new JenkinsRule();

    private static final OAEPParameterSpec OAEP_PARAMS =
            new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

    private KeyPair keyPair;
    private Path keyFile;
    private String originalKeyProperty;

    @Before
    public void setUp() throws Exception {
        originalKeyProperty = System.getProperty(io.jenkins.plugins.casc.impl.RSAEncryptionEngine.KEY_PROP);

        keyPair = generateKeyPair();
        keyFile = Files.createTempFile("casc-decrypt-lookup", ".pem");
        writePrivateKey(keyFile, keyPair.getPrivate());

        System.setProperty(
                io.jenkins.plugins.casc.impl.RSAEncryptionEngine.KEY_PROP,
                keyFile.toAbsolutePath().toString());
    }

    @After
    public void tearDown() throws IOException {
        String property = io.jenkins.plugins.casc.impl.RSAEncryptionEngine.KEY_PROP;

        if (originalKeyProperty == null) {
            System.clearProperty(property);
        } else {
            System.setProperty(property, originalKeyProperty);
        }

        if (keyFile != null) {
            Files.deleteIfExists(keyFile);
        }
    }

    @Test
    public void lookupDecryptsPayloadUsingMatchingEngine() throws GeneralSecurityException {
        String secret = "lookup-test-secret";
        String encrypted = encrypt(secret, keyPair.getPublic());

        String decrypted = DecryptLookup.INSTANCE.lookup("rsa:" + encrypted);

        assertThat(decrypted, equalTo(secret));
    }

    @Test
    public void lookupRejectsUnknownEngine() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> DecryptLookup.INSTANCE.lookup("unknown-engine:payload"));

        assertThat(exception.getMessage(), containsString("unknown-engine"));
    }

    @Test
    public void lookupRejectsMissingEnginePayloadSeparator() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> DecryptLookup.INSTANCE.lookup("rsa"));

        assertThat(exception.getMessage(), containsString("Invalid decrypt syntax"));
    }

    private static KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String encrypt(String plaintext, PublicKey publicKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_PARAMS);

        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(encrypted);
    }

    private static void writePrivateKey(Path path, PrivateKey privateKey) throws IOException {
        String encodedKey = Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(privateKey.getEncoded());

        String pem = "-----BEGIN PRIVATE KEY-----\n" + encodedKey + "\n-----END PRIVATE KEY-----\n";

        Files.writeString(path, pem, StandardCharsets.UTF_8);
    }
}
