package io.jenkins.plugins.casc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThrows;

import io.jenkins.plugins.casc.impl.RSAEncryptionEngine;
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
import org.junit.Test;

public class RSAEncryptionEngineTest {

    private static final OAEPParameterSpec OAEP_PARAMS =
            new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

    private RSAEncryptionEngine engine;
    private KeyPair keyPair;
    private Path keyFile;
    private String originalKeyProperty;

    @Before
    public void setUp() throws Exception {
        originalKeyProperty = System.getProperty(RSAEncryptionEngine.KEY_PROP);

        keyPair = generateKeyPair(2048);
        keyFile = Files.createTempFile("casc-rsa-key", ".pem");
        writePrivateKey(keyFile, keyPair.getPrivate());

        System.setProperty(
                RSAEncryptionEngine.KEY_PROP, keyFile.toAbsolutePath().toString());

        engine = new RSAEncryptionEngine();
    }

    @After
    public void tearDown() throws IOException {
        if (originalKeyProperty == null) {
            System.clearProperty(RSAEncryptionEngine.KEY_PROP);
        } else {
            System.setProperty(RSAEncryptionEngine.KEY_PROP, originalKeyProperty);
        }

        if (keyFile != null) {
            Files.deleteIfExists(keyFile);
        }
    }

    @Test
    public void decryptsValidOaepSha256Payload() throws GeneralSecurityException {
        String secret = "my-test-password";
        String ciphertext = encrypt(secret, keyPair.getPublic());

        assertThat(engine.decrypt(ciphertext), equalTo(secret));
    }

    @Test
    public void failsWhenCiphertextWasEncryptedWithAnotherKey() throws Exception {
        KeyPair otherKeyPair = generateKeyPair(2048);
        String ciphertext = encrypt("my-test-password", otherKeyPair.getPublic());

        assertThrows(GeneralSecurityException.class, () -> engine.decrypt(ciphertext));
    }

    @Test
    public void rejectsInvalidBase64Payload() {
        assertThrows(IllegalArgumentException.class, () -> engine.decrypt("not-valid-base64!"));
    }

    @Test
    public void failsWhenKeyConfigurationIsMissing() {
        System.setProperty(RSAEncryptionEngine.KEY_PROP, "");

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> new RSAEncryptionEngine().decrypt("AA=="));

        assertThat(exception.getMessage(), org.hamcrest.Matchers.containsString("not configured"));
    }

    @Test
    public void failsWhenKeyFileDoesNotExist() {
        System.setProperty(
                RSAEncryptionEngine.KEY_PROP,
                keyFile.resolveSibling("missing-private-key.pem")
                        .toAbsolutePath()
                        .toString());

        assertThrows(IllegalStateException.class, () -> new RSAEncryptionEngine().decrypt("AA=="));
    }

    @Test
    public void rejectsMalformedPemKey() throws IOException {
        Files.writeString(keyFile, "this is not a PEM private key", StandardCharsets.UTF_8);

        assertThrows(IllegalStateException.class, () -> new RSAEncryptionEngine().decrypt("AA=="));
    }

    @Test
    public void rejectsRsaKeySmallerThan2048Bits() throws Exception {
        KeyPair weakKeyPair = generateKeyPair(1024);
        writePrivateKey(keyFile, weakKeyPair.getPrivate());

        assertThrows(IllegalStateException.class, () -> new RSAEncryptionEngine().decrypt("AA=="));
    }

    @Test
    public void rejectsOversizedKeyFile() throws IOException {
        Files.write(keyFile, new byte[16 * 1024 + 1]);

        assertThrows(IllegalStateException.class, () -> new RSAEncryptionEngine().decrypt("AA=="));
    }

    private static KeyPair generateKeyPair(int keySize) throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(keySize);
        return generator.generateKeyPair();
    }

    private static String encrypt(String plaintext, PublicKey publicKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_PARAMS);

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(ciphertext);
    }

    private static void writePrivateKey(Path path, PrivateKey privateKey) throws IOException {
        String encodedKey = Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(privateKey.getEncoded());

        String pem = "-----BEGIN PRIVATE KEY-----\n" + encodedKey + "\n-----END PRIVATE KEY-----\n";

        Files.writeString(path, pem, StandardCharsets.UTF_8);
    }
}
