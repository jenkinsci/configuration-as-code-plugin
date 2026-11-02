package io.jenkins.plugins.casc;

import static com.nimbusds.jose.EncryptionMethod.A256GCM;
import static com.nimbusds.jose.JWEAlgorithm.RSA_OAEP_256;
import static io.jenkins.plugins.casc.impl.JweEncryptionEngine.KEY_PROP;
import static java.lang.System.clearProperty;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.write;
import static java.nio.file.Files.writeString;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.util.Base64.getMimeEncoder;
import static java.util.Base64.getUrlEncoder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSAEncrypter;
import io.jenkins.plugins.casc.impl.JweEncryptionEngine;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JweEncryptionEngineTest {

    private JweEncryptionEngine engine;
    private KeyPair keyPair;
    private Path keyFile;
    private String originalKeyProperty;

    @BeforeEach
    void setUp() throws Exception {
        originalKeyProperty = getProperty(KEY_PROP);

        keyPair = generateKeyPair(2048);
        keyFile = createTempFile("casc-rsa-key", ".pem");
        writePrivateKey(keyFile, keyPair.getPrivate());

        setProperty(KEY_PROP, keyFile.toAbsolutePath().toString());

        engine = new JweEncryptionEngine();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (originalKeyProperty == null) {
            clearProperty(KEY_PROP);
        } else {
            setProperty(KEY_PROP, originalKeyProperty);
        }

        if (keyFile != null) {
            deleteIfExists(keyFile);
        }
    }

    @Test
    void rejectsMissingKidInHeader() throws Exception {
        JWEHeader header = new JWEHeader.Builder(RSA_OAEP_256, A256GCM).build();
        JWEObject jwe = new JWEObject(header, new Payload("secret"));
        jwe.encrypt(new RSAEncrypter((RSAPublicKey) keyPair.getPublic()));

        String ciphertext = jwe.serialize();

        assertThrows(GeneralSecurityException.class, () -> engine.decrypt(ciphertext));
    }

    @Test
    void decryptsValidOaepSha256PayloadWithMatchingKid() throws Exception {
        String secret = "my-test-password";
        String kid = computeKeyId(keyPair.getPrivate());
        String ciphertext = encrypt(secret, keyPair.getPublic(), kid);

        assertEquals(secret, engine.decrypt(ciphertext));
    }

    @Test
    void failsWhenKidDoesNotMatchLocalKeys() throws Exception {
        String ciphertext = encrypt("my-test-password", keyPair.getPublic(), "non-existent-kid");

        assertThrows(GeneralSecurityException.class, () -> engine.decrypt(ciphertext));
    }

    @Test
    void decryptsMultipleKeysInSinglePemFileAfterFileChanges() throws Exception {
        KeyPair newKeyPair = generateKeyPair(2048);
        String newKid = computeKeyId(newKeyPair.getPrivate());

        String secretOldKey = "old-secret";
        String ciphertextOld = encrypt(secretOldKey, keyPair.getPublic(), computeKeyId(keyPair.getPrivate()));

        assertEquals(secretOldKey, engine.decrypt(ciphertextOld));

        writeString(keyFile, formatPrivateKeyPem(newKeyPair.getPrivate()), UTF_8, APPEND);

        String secretNewKey = "new-secret";
        String ciphertextNew = encrypt(secretNewKey, newKeyPair.getPublic(), newKid);

        assertEquals(secretOldKey, engine.decrypt(ciphertextOld));
        assertEquals(secretNewKey, engine.decrypt(ciphertextNew));
    }

    @Test
    void stopsUsingRemovedKeysAfterFileChanges() throws Exception {
        String kid = computeKeyId(keyPair.getPrivate());
        String ciphertext = encrypt("my-test-password", keyPair.getPublic(), kid);

        assertEquals("my-test-password", engine.decrypt(ciphertext));

        writeString(keyFile, "", UTF_8);

        assertThrows(IllegalStateException.class, () -> engine.decrypt(ciphertext));
    }

    @Test
    void doesNotUseCachedKeysWhenReplacementFileIsInvalid() throws Exception {
        String kid = computeKeyId(keyPair.getPrivate());
        String ciphertext = encrypt("my-test-password", keyPair.getPublic(), kid);

        assertEquals("my-test-password", engine.decrypt(ciphertext));

        writeString(keyFile, "invalid PEM content", UTF_8);

        assertThrows(IllegalStateException.class, () -> engine.decrypt(ciphertext));
    }

    @Test
    void failsWhenCiphertextWasEncryptedWithAnotherKey() throws Exception {
        KeyPair otherKeyPair = generateKeyPair(2048);
        String kid = computeKeyId(keyPair.getPrivate());
        String ciphertext = encrypt("my-test-password", otherKeyPair.getPublic(), kid);

        assertThrows(GeneralSecurityException.class, () -> engine.decrypt(ciphertext));
    }

    @Test
    void rejectsInvalidJwePayload() {
        assertThrows(IllegalArgumentException.class, () -> engine.decrypt("not.a.valid.jwe.format"));
    }

    @Test
    void failsWhenKeyConfigurationIsMissing() throws Exception {
        setProperty(KEY_PROP, "");
        String kid = computeKeyId(keyPair.getPrivate());
        String ciphertext = encrypt("secret", keyPair.getPublic(), kid);

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> new JweEncryptionEngine().decrypt(ciphertext));

        assertTrue(exception.getMessage().contains("not configured"));
    }

    @Test
    void failsWhenKeyFileDoesNotExist() throws Exception {
        setProperty(
                KEY_PROP,
                keyFile.resolveSibling("missing-private-key.pem")
                        .toAbsolutePath()
                        .toString());

        String kid = computeKeyId(keyPair.getPrivate());
        String ciphertext = encrypt("secret", keyPair.getPublic(), kid);

        assertThrows(IllegalStateException.class, () -> new JweEncryptionEngine().decrypt(ciphertext));
    }

    @Test
    void rejectsMalformedPemKey() throws Exception {
        writeString(keyFile, "this is not a PEM private key", UTF_8);

        String kid = computeKeyId(keyPair.getPrivate());
        String ciphertext = encrypt("secret", keyPair.getPublic(), kid);

        assertThrows(IllegalStateException.class, () -> new JweEncryptionEngine().decrypt(ciphertext));
    }

    @Test
    void rejectsRsaKeySmallerThan2048Bits() throws Exception {
        KeyPair weakKeyPair = generateKeyPair(1024);
        writePrivateKey(keyFile, weakKeyPair.getPrivate());

        String kid = computeKeyId(keyPair.getPrivate());
        String ciphertext = encrypt("secret", keyPair.getPublic(), kid);

        assertThrows(IllegalStateException.class, () -> new JweEncryptionEngine().decrypt(ciphertext));
    }

    @Test
    void rejectsOversizedKeyFile() throws Exception {
        write(keyFile, new byte[64 * 1024 + 1]);

        String kid = computeKeyId(keyPair.getPrivate());
        String ciphertext = encrypt("secret", keyPair.getPublic(), kid);

        assertThrows(IllegalStateException.class, () -> new JweEncryptionEngine().decrypt(ciphertext));
    }

    @Test
    void rejectsSpecialFileWithoutReadingUnboundedData() throws Exception {
        Path zeroFile = Path.of("/dev/zero");
        assumeTrue(exists(zeroFile) && isReadable(zeroFile));

        setProperty(KEY_PROP, zeroFile.toAbsolutePath().toString());

        String kid = computeKeyId(keyPair.getPrivate());
        String ciphertext = encrypt("secret", keyPair.getPublic(), kid);

        assertThrows(IllegalStateException.class, () -> new JweEncryptionEngine().decrypt(ciphertext));
    }

    @Test
    void rejectsDirectoryAsKeyFile() throws Exception {
        Path directory = createTempDirectory("casc-key-directory");

        try {
            setProperty(KEY_PROP, directory.toString());

            String kid = computeKeyId(keyPair.getPrivate());
            String ciphertext = encrypt("secret", keyPair.getPublic(), kid);

            IllegalStateException exception =
                    assertThrows(IllegalStateException.class, () -> new JweEncryptionEngine().decrypt(ciphertext));

            assertTrue(exception.getMessage().contains("regular file"));
            assertTrue(
                    exception.getMessage().contains(directory.toAbsolutePath().toString()));
        } finally {
            deleteIfExists(directory);
        }
    }

    @Test
    void decryptsExternalJweFixtureSuccessfully() throws Exception {
        KeyPair externalKeyPair = generateKeyPair(2048);

        String pemKey = formatPrivateKeyPem(externalKeyPair.getPrivate());
        writeString(keyFile, pemKey, UTF_8);

        String kid = computeKeyId(externalKeyPair.getPrivate());
        String externalJwe = encrypt("openssl-interop-secret", externalKeyPair.getPublic(), kid);

        assertEquals("openssl-interop-secret", engine.decrypt(externalJwe));
    }

    private static KeyPair generateKeyPair(int keySize) throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(keySize);
        return generator.generateKeyPair();
    }

    private static String encrypt(String plaintext, PublicKey publicKey, String kid) throws Exception {
        JWEHeader header =
                new JWEHeader.Builder(RSA_OAEP_256, A256GCM).keyID(kid).build();

        JWEObject jwe = new JWEObject(header, new Payload(plaintext));
        jwe.encrypt(new RSAEncrypter((RSAPublicKey) publicKey));

        return jwe.serialize();
    }

    private static String formatPrivateKeyPem(PrivateKey privateKey) {
        String encodedKey = getMimeEncoder(64, new byte[] {'\n'}).encodeToString(privateKey.getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + encodedKey + "\n-----END PRIVATE KEY-----\n";
    }

    private static void writePrivateKey(Path path, PrivateKey privateKey) throws IOException {
        writeString(path, formatPrivateKeyPem(privateKey), UTF_8);
    }

    private static String computeKeyId(PrivateKey privateKey) throws GeneralSecurityException {
        RSAPrivateCrtKey key = (RSAPrivateCrtKey) privateKey;
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(key.getModulus(), key.getPublicExponent());
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(publicKey.getEncoded());
        byte[] truncated = Arrays.copyOf(hash, 8);

        return getUrlEncoder().withoutPadding().encodeToString(truncated);
    }
}
