package io.jenkins.plugins.casc;

import static com.nimbusds.jose.CompressionAlgorithm.DEF;
import static com.nimbusds.jose.EncryptionMethod.A128GCM;
import static com.nimbusds.jose.EncryptionMethod.A256GCM;
import static com.nimbusds.jose.JWEAlgorithm.RSA_OAEP_256;
import static com.nimbusds.jose.JWEAlgorithm.RSA_OAEP_384;
import static io.jenkins.plugins.casc.impl.JweEncryptionEngine.KEY_PROP;
import static java.lang.String.format;
import static java.lang.System.clearProperty;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.readString;
import static java.nio.file.Files.write;
import static java.nio.file.Files.writeString;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.util.Arrays.fill;
import static java.util.Base64.getDecoder;
import static java.util.Base64.getMimeEncoder;
import static java.util.Base64.getUrlEncoder;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;
import io.jenkins.plugins.casc.impl.JweEncryptionEngine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.CharacterCodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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

    @Test
    void decryptsNodeJoseInteropFixtureSuccessfully() throws Exception {
        String fixture = readString(
                        Paths.get(requireNonNull(getClass().getResource("node-jose-interop-secret.jwe"))
                                .toURI()),
                        UTF_8)
                .trim();

        String[] segments = fixture.split("\\.");
        assertEquals(5, segments.length, "JWE compact serialization must contain exactly 5 segments");

        JWEObject jweObject = JWEObject.parse(fixture);
        JWEHeader header = jweObject.getHeader();
        assertEquals(RSA_OAEP_256, header.getAlgorithm());
        assertEquals(A256GCM, header.getEncryptionMethod());
        assertEquals("-C_Vr8I_0MI", header.getKeyID());

        String secretKeyPem = readString(
                Paths.get(requireNonNull(getClass().getResource("secret.key")).toURI()), UTF_8);
        PrivateKey privateKey = parsePrivateKey(secretKeyPem);
        assertEquals(header.getKeyID(), computeKeyId(privateKey));

        writePrivateKey(keyFile, privateKey);

        assertEquals("node-jose-interop-secret", engine.decrypt(fixture));
    }

    @Test
    void rejectsOversizedJwePayload() {
        int maxJweSize = 10 * 1024 * 1024;

        char[] oversizedChars = new char[maxJweSize + 1];
        fill(oversizedChars, 'a');

        String oversizedJwe = new String(oversizedChars);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> engine.decrypt(oversizedJwe));

        assertEquals("JWE payload exceeds the maximum allowed size.", exception.getMessage());
    }

    @Test
    void rejectsDecryptedPayloadThatIsNotValidUtf8() throws Exception {
        byte[] invalidUtf8 = {(byte) 0xC3, (byte) 0x28};

        String kid = computeKeyId(keyPair.getPrivate());

        JWEHeader header =
                new JWEHeader.Builder(RSA_OAEP_256, A256GCM).keyID(kid).build();

        JWEObject jwe = new JWEObject(header, new Payload(invalidUtf8));
        jwe.encrypt(new RSAEncrypter((RSAPublicKey) keyPair.getPublic()));

        String ciphertext = jwe.serialize();

        GeneralSecurityException exception =
                assertThrows(GeneralSecurityException.class, () -> engine.decrypt(ciphertext));

        assertEquals("Decrypted JWE payload is not valid UTF-8.", exception.getMessage());

        assertInstanceOf(CharacterCodingException.class, exception.getCause());
    }

    @Test
    void rejectsUnsupportedKeyManagementAlgorithm() throws Exception {
        JWEHeader header = new JWEHeader.Builder(RSA_OAEP_384, A256GCM)
                .keyID(computeKeyId(keyPair.getPrivate()))
                .build();

        JWEObject jwe = new JWEObject(header, new Payload("secret"));
        jwe.encrypt(new RSAEncrypter((RSAPublicKey) keyPair.getPublic()));

        String ciphertext = jwe.serialize();

        GeneralSecurityException exception =
                assertThrows(GeneralSecurityException.class, () -> engine.decrypt(ciphertext));

        assertEquals("Unsupported JWE key management algorithm. Expected RSA-OAEP-256.", exception.getMessage());
    }

    @Test
    void rejectsUnsupportedContentEncryptionAlgorithm() throws Exception {
        JWEHeader header = new JWEHeader.Builder(RSA_OAEP_256, A128GCM)
                .keyID(computeKeyId(keyPair.getPrivate()))
                .build();

        JWEObject jwe = new JWEObject(header, new Payload("secret"));
        jwe.encrypt(new RSAEncrypter((RSAPublicKey) keyPair.getPublic()));

        String ciphertext = jwe.serialize();

        GeneralSecurityException exception =
                assertThrows(GeneralSecurityException.class, () -> engine.decrypt(ciphertext));

        assertEquals("Unsupported JWE content encryption algorithm. Expected A256GCM.", exception.getMessage());
    }

    @Test
    void rejectsJweCompression() throws Exception {
        JWEHeader header = new JWEHeader.Builder(RSA_OAEP_256, A256GCM)
                .keyID(computeKeyId(keyPair.getPrivate()))
                .compressionAlgorithm(DEF)
                .build();

        JWEObject jwe = new JWEObject(header, new Payload("secret"));
        jwe.encrypt(new RSAEncrypter((RSAPublicKey) keyPair.getPublic()));

        String ciphertext = jwe.serialize();

        GeneralSecurityException exception =
                assertThrows(GeneralSecurityException.class, () -> engine.decrypt(ciphertext));

        assertEquals("JWE compression is not supported.", exception.getMessage());
    }

    @Test
    void rejectsCriticalJweHeaders() throws Exception {
        JWEHeader header = new JWEHeader.Builder(RSA_OAEP_256, A256GCM)
                .keyID(computeKeyId(keyPair.getPrivate()))
                .criticalParams(Set.of("test"))
                .customParam("test", "value")
                .build();

        JWEObject jwe = new JWEObject(header, new Payload("secret"));
        jwe.encrypt(new RSAEncrypter((RSAPublicKey) keyPair.getPublic()));

        String ciphertext = jwe.serialize();

        GeneralSecurityException exception =
                assertThrows(GeneralSecurityException.class, () -> engine.decrypt(ciphertext));

        assertEquals("Critical JWE headers are not supported.", exception.getMessage());
    }

    @Test
    void rejectsBlankKidInHeader() throws Exception {
        JWEHeader header =
                new JWEHeader.Builder(RSA_OAEP_256, A256GCM).keyID(" ").build();

        JWEObject jwe = new JWEObject(header, new Payload("secret"));
        jwe.encrypt(new RSAEncrypter((RSAPublicKey) keyPair.getPublic()));

        String ciphertext = jwe.serialize();

        GeneralSecurityException exception =
                assertThrows(GeneralSecurityException.class, () -> engine.decrypt(ciphertext));

        assertEquals("JWE protected header must contain a key ID (kid).", exception.getMessage());
    }

    @Test
    void rejectsUnreadableKeyFile() throws Exception {
        Path unreadableFile = createTempFile("casc-unreadable-key", ".pem");

        try {
            writePrivateKey(unreadableFile, keyPair.getPrivate());

            boolean permissionsChanged = unreadableFile.toFile().setReadable(false, false);

            assumeTrue(permissionsChanged && !isReadable(unreadableFile));

            setProperty(KEY_PROP, unreadableFile.toAbsolutePath().toString());

            String kid = computeKeyId(keyPair.getPrivate());
            String ciphertext = encrypt("secret", keyPair.getPublic(), kid);

            IllegalStateException exception =
                    assertThrows(IllegalStateException.class, () -> engine.decrypt(ciphertext));

            assertTrue(exception.getMessage().contains("not readable"));
            assertTrue(exception
                    .getMessage()
                    .contains(unreadableFile.toAbsolutePath().toString()));
        } finally {
            if (!unreadableFile.toFile().setReadable(true, true)) {
                unreadableFile.toFile().deleteOnExit();
            }
            deleteIfExists(unreadableFile);
        }
    }

    @Test
    void rejectsPemWithMalformedBeginMarker() throws Exception {
        writeString(keyFile, "-----BEGIN PRIVATE KEY", UTF_8);

        String kid = computeKeyId(keyPair.getPrivate());
        String ciphertext = encrypt("secret", keyPair.getPublic(), kid);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> engine.decrypt(ciphertext));

        assertTrue(
                exception
                        .getMessage()
                        .startsWith(
                                "Failed to parse the PKCS#8 RSA private keys. Ensure they are valid, unencrypted PKCS#8 PEM blocks."),
                "Message should start with base error");

        assertInstanceOf(IllegalArgumentException.class, exception.getCause());

        assertEquals(
                "Malformed PEM block: missing end of BEGIN marker.",
                exception.getCause().getMessage());
    }

    @Test
    void rejectsTraditionalPkcs1RsaPrivateKey() throws Exception {
        writeString(keyFile, """
                -----BEGIN RSA PRIVATE KEY-----
                invalid-key-data
                -----END RSA PRIVATE KEY-----
                """, UTF_8);

        String kid = computeKeyId(keyPair.getPrivate());
        String ciphertext = encrypt("secret", keyPair.getPublic(), kid);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> engine.decrypt(ciphertext));

        assertTrue(
                exception
                        .getMessage()
                        .startsWith(
                                "Failed to parse the PKCS#8 RSA private keys. Ensure they are valid, unencrypted PKCS#8 PEM blocks."),
                "Message should start with base error");

        assertInstanceOf(IllegalArgumentException.class, exception.getCause());

        assertEquals(
                "Traditional PKCS#1 RSA keys are not supported. Please convert to PKCS#8 format.",
                exception.getCause().getMessage());
    }

    @Test
    void rejectsUnsupportedPemBlockType() throws Exception {
        writeString(keyFile, """
                -----BEGIN ENCRYPTED PRIVATE KEY-----
                invalid-key-data
                -----END ENCRYPTED PRIVATE KEY-----
                """, UTF_8);

        String kid = computeKeyId(keyPair.getPrivate());
        String ciphertext = encrypt("secret", keyPair.getPublic(), kid);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> engine.decrypt(ciphertext));

        assertTrue(
                exception
                        .getMessage()
                        .startsWith(
                                "Failed to parse the PKCS#8 RSA private keys. Ensure they are valid, unencrypted PKCS#8 PEM blocks."),
                "Message should start with base error");

        assertInstanceOf(IllegalArgumentException.class, exception.getCause());

        assertEquals(
                "Unexpected PEM block type: ENCRYPTED PRIVATE KEY",
                exception.getCause().getMessage());
    }

    @Test
    void rejectsPemWithMissingEndMarker() throws Exception {
        writeString(keyFile, """
                -----BEGIN PRIVATE KEY-----
                some-base64-data
                """, UTF_8);

        String kid = computeKeyId(keyPair.getPrivate());
        String ciphertext = encrypt("secret", keyPair.getPublic(), kid);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> engine.decrypt(ciphertext));

        assertTrue(
                exception
                        .getMessage()
                        .startsWith(
                                "Failed to parse the PKCS#8 RSA private keys. Ensure they are valid, unencrypted PKCS#8 PEM blocks."),
                "Message should start with base error");

        assertInstanceOf(IllegalArgumentException.class, exception.getCause());

        assertEquals(
                "Incomplete PEM block missing end marker.", exception.getCause().getMessage());
    }

    @Test
    void rejectsNonCrtRsaPrivateKey() throws Exception {
        RSAPrivateCrtKey crtKey = (RSAPrivateCrtKey) keyPair.getPrivate();
        RSAPrivateKeySpec nonCrtSpec = new RSAPrivateKeySpec(crtKey.getModulus(), crtKey.getPrivateExponent());

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey nonCrtKey = keyFactory.generatePrivate(nonCrtSpec);

        writePrivateKey(keyFile, nonCrtKey);

        String kid = computeKeyId(keyPair.getPrivate());
        String ciphertext = encrypt("secret", keyPair.getPublic(), kid);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> engine.decrypt(ciphertext));

        assertTrue(
                exception
                        .getMessage()
                        .startsWith(
                                "Failed to parse the PKCS#8 RSA private keys. Ensure they are valid, unencrypted PKCS#8 PEM blocks."),
                "Message should start with base error");

        assertInstanceOf(IllegalArgumentException.class, exception.getCause());

        assertEquals(
                "The provided PKCS#8 key is not an RSA CRT key.",
                exception.getCause().getMessage());
    }

    @Test
    void rejectsDuplicateRsaKeyIds() throws Exception {
        String pemKey = formatPrivateKeyPem(keyPair.getPrivate());

        writeString(keyFile, pemKey + pemKey, UTF_8);

        String kid = computeKeyId(keyPair.getPrivate());
        String ciphertext = encrypt("secret", keyPair.getPublic(), kid);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> engine.decrypt(ciphertext));

        assertTrue(
                exception
                        .getMessage()
                        .startsWith(
                                "Failed to parse the PKCS#8 RSA private keys. Ensure they are valid, unencrypted PKCS#8 PEM blocks."),
                "Message should start with base error");

        assertInstanceOf(IllegalArgumentException.class, exception.getCause());

        assertEquals("Duplicate RSA key ID found: " + kid, exception.getCause().getMessage());
    }

    @Test
    void decryptsJweWithTypHeader() throws Exception {
        String secret = "secret-with-typ";
        String kid = computeKeyId(keyPair.getPrivate());

        JWEHeader header = new JWEHeader.Builder(RSA_OAEP_256, A256GCM)
                .keyID(kid)
                .type(new JOSEObjectType("JWE"))
                .build();

        String ciphertext = encrypt(secret, keyPair.getPublic(), header);

        assertEquals(secret, engine.decrypt(ciphertext));
    }

    @Test
    void decryptsJweWithCtyHeader() throws Exception {
        String secret = "secret-with-cty";
        String kid = computeKeyId(keyPair.getPrivate());

        JWEHeader header = new JWEHeader.Builder(RSA_OAEP_256, A256GCM)
                .keyID(kid)
                .contentType("JWT")
                .build();

        String ciphertext = encrypt(secret, keyPair.getPublic(), header);

        assertEquals(secret, engine.decrypt(ciphertext));
    }

    @Test
    void decryptsJweWithTypAndCtyHeaders() throws Exception {
        String secret = "secret-with-typ-and-cty";
        String kid = computeKeyId(keyPair.getPrivate());

        JWEHeader header = new JWEHeader.Builder(RSA_OAEP_256, A256GCM)
                .keyID(kid)
                .type(new JOSEObjectType("JWE"))
                .contentType("JWT")
                .build();

        String ciphertext = encrypt(secret, keyPair.getPublic(), header);

        assertEquals(secret, engine.decrypt(ciphertext));
    }

    @Test
    void parsesKeyFileWithUtf8BomHeader() throws Exception {
        String pemKey = "\uFEFF" + formatPrivateKeyPem(keyPair.getPrivate());
        writeString(keyFile, pemKey, UTF_8);

        String secret = "secret-with-bom";
        String kid = computeKeyId(keyPair.getPrivate());
        String ciphertext = encrypt(secret, keyPair.getPublic(), kid);

        assertEquals(secret, engine.decrypt(ciphertext));
    }

    @Test
    void rejectsArbitraryTextBeforePemBlock() throws Exception {
        String keyContent = "Bag Attributes\n    friendlyName: my-key\n" + formatPrivateKeyPem(keyPair.getPrivate());
        writeString(keyFile, keyContent, UTF_8);

        String kid = computeKeyId(keyPair.getPrivate());
        String ciphertext = encrypt("secret", keyPair.getPublic(), kid);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> engine.decrypt(ciphertext));

        assertTrue(
                exception.getMessage().contains("Unexpected or malformed content found outside of PEM blocks"),
                "Arbitrary text before PEM blocks must be explicitly rejected");
    }

    @Test
    void rejectsArbitraryTextBetweenPemBlocks() throws Exception {
        KeyPair keyPair2 = generateKeyPair(2048);
        String keyContent = formatPrivateKeyPem(keyPair.getPrivate())
                + "\n# Some metadata text between blocks\n"
                + formatPrivateKeyPem(keyPair2.getPrivate());
        writeString(keyFile, keyContent, UTF_8);

        String kid = computeKeyId(keyPair.getPrivate());
        String ciphertext = encrypt("secret", keyPair.getPublic(), kid);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> engine.decrypt(ciphertext));

        assertTrue(
                exception.getMessage().contains("Unexpected or malformed content found outside of PEM blocks"),
                "Arbitrary text between PEM blocks must be explicitly rejected");
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "\t", "\r", "\n"})
    void rejectsJweContainingWhitespace(String whitespace) throws Exception {
        String kid = computeKeyId(keyPair.getPrivate());
        String encrypted = encrypt("test", keyPair.getPublic(), kid);
        String wrapped = encrypted.substring(0, 20) + whitespace + encrypted.substring(20);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> engine.decrypt(wrapped));

        assertTrue(
                exception.getMessage().contains("must not contain whitespace"),
                "Exception message should mention whitespace restriction");
    }

    @ParameterizedTest
    @MethodSource("missingHeaderJwes")
    void rejectsMissingAlgOrEncInHeader(String invalidJwe) {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> engine.decrypt(invalidJwe));

        assertTrue(exception.getMessage().contains("Failed to parse JWE payload"));
    }

    @ParameterizedTest
    @MethodSource("unsupportedHeaders")
    void rejectsUnsupportedJweHeaderParameters(JWEHeader header) throws Exception {
        String ciphertext = encrypt("secret", keyPair.getPublic(), header);

        GeneralSecurityException exception =
                assertThrows(GeneralSecurityException.class, () -> engine.decrypt(ciphertext));

        assertTrue(exception.getMessage().contains("unsupported parameters"));
    }

    @ParameterizedTest
    @MethodSource("emptyPayloads")
    void rejectsNullOrBlankJwePayload(String encryptedData) {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> engine.decrypt(encryptedData));

        assertEquals("JWE payload must not be empty.", exception.getMessage());
    }

    private static Stream<Arguments> missingHeaderJwes() {
        String dummy = new Base64URL("dummy").toString();
        String missingAlg = new Base64URL("{\"enc\":\"A256GCM\",\"kid\":\"test-kid\"}").toString();
        String missingEnc = new Base64URL("{\"alg\":\"RSA-OAEP-256\",\"kid\":\"test-kid\"}").toString();

        return Stream.of(
                Arguments.of(format("%s.%s.%s.%s.%s", missingAlg, dummy, dummy, dummy, dummy)),
                Arguments.of(format("%s.%s.%s.%s.%s", missingEnc, dummy, dummy, dummy, dummy)));
    }

    private static Stream<Arguments> unsupportedHeaders() throws Exception {
        String kid = "test-kid";

        JWK dummyJwk = new RSAKey.Builder(new Base64URL("dummy-n"), new Base64URL("dummy-e")).build();

        return Stream.of(
                arguments(new JWEHeader.Builder(RSA_OAEP_256, A256GCM)
                        .keyID(kid)
                        .jwkURL(new URI("https://example.com/jku"))
                        .build()),
                arguments(new JWEHeader.Builder(RSA_OAEP_256, A256GCM)
                        .keyID(kid)
                        .jwk(dummyJwk)
                        .build()),
                arguments(new JWEHeader.Builder(RSA_OAEP_256, A256GCM)
                        .keyID(kid)
                        .x509CertURL(new URI("https://example.com/x5u"))
                        .build()),
                arguments(new JWEHeader.Builder(RSA_OAEP_256, A256GCM)
                        .keyID(kid)
                        .x509CertChain(List.of(new Base64("dummy-cert-data")))
                        .build()),
                arguments(new JWEHeader.Builder(RSA_OAEP_256, A256GCM)
                        .keyID(kid)
                        .customParam("custom", "value")
                        .build()));
    }

    private static Stream<Arguments> emptyPayloads() {
        return Stream.of(arguments((String) null), arguments(""), arguments(" "), arguments("\t\n"));
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

    private static String encrypt(String plaintext, PublicKey publicKey, JWEHeader header) throws Exception {
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

    private static PrivateKey parsePrivateKey(String pem) throws GeneralSecurityException {
        String base64 = pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s+", "");
        byte[] decoded = getDecoder().decode(base64);
        if (pem.contains("BEGIN RSA PRIVATE KEY")) {
            decoded = wrapPkcs1ToPkcs8(decoded);
        }
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private static byte[] wrapPkcs1ToPkcs8(byte[] pkcs1) {
        byte[] algId = new byte[] {
            0x30,
            0x0d,
            0x06,
            0x09,
            0x2a,
            (byte) 0x86,
            0x48,
            (byte) 0x86,
            (byte) 0xf7,
            0x0d,
            0x01,
            0x01,
            0x01,
            0x05,
            0x00
        };
        byte[] version = new byte[] {0x02, 0x01, 0x00};

        ByteArrayOutputStream octetStream = new ByteArrayOutputStream();
        octetStream.write(0x04);
        writeDerLength(octetStream, pkcs1.length);
        octetStream.write(pkcs1, 0, pkcs1.length);
        byte[] octetBytes = octetStream.toByteArray();

        ByteArrayOutputStream seqBody = new ByteArrayOutputStream();
        seqBody.write(version, 0, version.length);
        seqBody.write(algId, 0, algId.length);
        seqBody.write(octetBytes, 0, octetBytes.length);
        byte[] body = seqBody.toByteArray();

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        result.write(0x30);
        writeDerLength(result, body.length);
        result.write(body, 0, body.length);
        return result.toByteArray();
    }

    private static void writeDerLength(ByteArrayOutputStream out, int length) {
        if (length < 128) {
            out.write(length);
        } else if (length < 256) {
            out.write(0x81);
            out.write(length);
        } else {
            out.write(0x82);
            out.write((length >> 8) & 0xff);
            out.write(length & 0xff);
        }
    }
}
